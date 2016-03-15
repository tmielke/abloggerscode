# Test case for consumer starvation in ActiveMQ 5.x

This JUnit test intends to show how some slow or inactive queue destinations
can starve consumers on other destinations in ActiveMQ.

The test uses three destinations:
`Dest1-no-consumer` - has no consumer attached.
`Dest2-slow-consumer` -  has a slow consumer attached
`Dest3-fast-consumer` - has a fast consumer attached

In addition the broker is configured for
 
```xml
<memoryUsage limit="10 mb" />
```

but does not configure for destination wide memory limits via the `<policyEntry>`
config. That implies all destinations compete to use the brokers memory until 
the specified limit is reached.
A broker wide memoryUsage limit of 10MB sounds unrealistic but is only used to
reproduce the problem quickly. The same problem can be reproduced with any 
higher memoryLimit but will take longer.


Now with this configuration a producer connects to queue 'Dest1-no-consumer'
and sends 7 messages a 1 MB. This moves the broker's MemoryPercentUsage JMX
attribute to 70%, the magic limit at which a destination cursor stops to accept
messages in its cache.

Once that producer has finished, a second producer connects to 
'Dest2-slow-consumer' and a third producer just 500 msecs later connects to 
queue 'Dest3-fast-consumer'. Both send 30 messages a 1MB in a 3 second
interval.
Only thereafter are the consumers for these two destinations created.
The time of these events ensures that the consumer for Dest2-slow-consumer
connects to the broker first followed by the consumer for 'Dest3-fast-consumer'.

Because of the broker's MemoryPercentUsage being 70% after the 7 msgs sent to 
'Dest1-no-consumer', the messages sent to the other two destinations will not
be held in the cursor cache but will need to be loaded from the store at dispatch
time.
When the first consumer for queue 'Dest2-slow-consumer' connects, the broker will
load maxPageSize messages from store to cursor cache. This makes the broker's
memoryPercentUsage climb above 100%. It does not need to climb above 100%, above 
70% is already enough for this problem to reproduce.
When the second consumer for queue 'Dest3-fast-consumer' connects, the broker 
cannot load any more messages from its persistence store to the cursor cache 
because the broker's MemoryPercentUsage is already above 70%, the tipping 
point at which no more messages are loaded into the cursors. The consumer for
queue 'Dest3-fast-consumer' gets starved and will not receive any messages,
despite its queue not being empty.
The consumer for 'Dest3-fast-consumer' will log this message repeatedly

```
I got starved and did not receive a message for queue Dest3-fast-consumer despite its queue size being 27
```

indicating that it did not receive a single message within the last 3 seconds
interval.

This problem continues until the slow consumer for queue 'Dest2-slow-consumer' 
has consumed all 30 messages. And even thereafter the brokers MemoryPercentUsage
may still be at 70% preventing the fast consumer to receive any messages.

To finally get the consumer for 'Dest3-fast-consumer' receive all of its 
messages, it is necessary to consume one single message from 
'Dest1-no-consumer', so that the brokers MemoryPercentUsage decreases below
70% and now the cursor for queue 'Dest3-fast-consumer' can load more message
from store into its cursor cache. That will finally make the consumer receive
all messages from queue 'Dest3-fast-consumer'.


The important message to take away is that destinations with no or rather slow 
consumers may starve fast consumers on other destinations if no destination 
limit is configured on every destination.
Setting a destination limit can prevent this problem. However in some situations
this may still happen with destination limits in place but the problem is much
less likely to occur then.

This all only applies to persistent queue messages only, not to non-persistent 
queue messages and not to topic messages where the cursors behave differently.


## COMPILING
mvn clean test-compile


## RUNNING
mvn test and carefully observe the logging output.
