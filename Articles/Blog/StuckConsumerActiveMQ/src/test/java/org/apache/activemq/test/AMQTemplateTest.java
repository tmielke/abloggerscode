/**
 * 
 */
package org.apache.activemq.test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.activemq.broker.BrokerService;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/**
 * Unit test.
 * See documentation of method testConsumerStarvation() for details.
 */
public class AMQTemplateTest {

    protected final static String CONFIG_FILE="AMQTemplateTest.xml";
    protected final static String BROKERURL = "tcp://localhost:61616?jms.prefetchPolicy.all=1";
    protected final static String DEST1 = "Stomp1";
    protected final static String DEST2 = "Stomp2";
    protected final static int NUM_MSGS = 1;
    protected static Logger LOG = LoggerFactory.getLogger(AMQTemplateTest.class);
    private BrokerService broker = null;


    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        LOG.info("Starting up");

        // start up broker instance
        try {
            Thread.currentThread().setContextClassLoader(AMQTemplateTest.class.getClassLoader());
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(CONFIG_FILE);

            //resolve broker
            broker = (BrokerService) context.getBean("broker");

            //start the broker  
            if (!broker.isStarted()) {
                LOG.info("Broker broker not yet started. Kicking it off now.");
                broker.start();
            }
            else {
                LOG.info("Broker broker already started. Not kicking it off a second time.");
            }
        }
        catch(Exception e) {
            LOG.error("Error: " + e.getMessage());
            throw e;
        }
    }


    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        LOG.info("Shutting down");
        if (broker != null && broker.isStarted()) {
            LOG.info("Broker still running, stopping it now.");
            broker.stop();
        }
        else {
            LOG.info("Broker not running, nothing to shutdown.");
        }
    }



    /**
     * Shows how one or two slow or inactive destinations can starve consumers
     * on other destinations.
     * 
     * Broker is configured with
     * <memoryUsage limit="10 mb" />
     * There are no destination limits configured, so all destinations 
     * compete for the brokers overall memory.
     *
     * This test first sends 7 messages a 1MB to queue 'Dest1-no-consumer'.
     * This destination has no consumers. MemoryPercentUsage for the 
     * broker climbs up to 70%.
     * 
     * Next 5 messages a 1MB are sent to queue 'Dest2-slow-consumer'.
     * Next 1 message a 1MB is sent to queue 'Dest3-fast-consumer'.
     * 
     * Then we create two producers that each sent 30 1MB messages to 
     * queues 'Dest2-slow-consumer' and 'Dest3-fast-consumer' at 3 seconds
     * interval. 
     * 
     * Thereafter we create one slow consumer for 'Dest2-slow-consumer' that
     * consumes one messages every 5 seconds. Once the consumer is connected 
     * the cursor for queue 'Dest2-slow-consumer' will try to load messages 
     * from store and fill the MemoryUsage > 100%.
     * 
     * Finally we create a fast consumer for queue 'Dest3-fast-consumer' that
     * consumes messages as fast as possible.
     * However this consumer will not receive any messages until all messages of 
     * queue 'Dest2-slow-consumer' were consumed. And even thereafter it *may* 
     * remain stuck as the first queue 'Dest1-no-consumer' still holds 70% of 
     * the brokers MemoryPercentUsage.
     * 
     * Logging output is given from each consumer. In particular the
     * consumer for queue 'Dest3-fast-consumer' will print this line 
     * every 3 seconds
     * WARN pool-2-thread-1 Consumer - I got starved and did not receive a message for queue Dest3-fast
     * 
     * although there are messages on the queue but they cannot be paged in 
     * from the persistence store as the brokers MemoryPercentUsage remains above 70%.
     * 
     * @throws Exception
     */
    @Test
    public void testConsumerStarvation() throws Exception {
        LOG.info("Testing for consumer starvation.");

        String dest1 = "Dest1-no-consumer";
        String dest2 = "Dest2-slow-consumer";
        String dest3 = "Dest3-fast-consumer";

        // sent 7 msgs a 1MB  each to queue1 so that 70% of MemoryUsage limit are reached
        Thread producerThread = new Thread(new Producer(7, dest1, 0, 1024, 0));
        producerThread.setName("Producer for " + dest1);
        producerThread.start();
        producerThread.join(10*1000);
        Thread.sleep(1000);

        /*
        // sent 1 msg of 1MB to queue3
        Thread producerThread3 = new Thread(new Producer(1, dest3, 0, 1024, 0));
        producerThread3.setName("Producer for " + dest3);
        producerThread3.start();
        producerThread3.join(10*1000);
        */
        // kick off producer for quuee2 and queue3 that periodically produces msgs
        Thread producerThread2 = new Thread(new Producer(30, dest2, 0, 1024, 3*1000));
        producerThread2.setName("Producer for " + dest2);
        producerThread2.start();
        Thread.sleep(500);

        Thread producerThread3 = new Thread(new Producer(30, dest3, 0, 1024, 3*1000));
        producerThread3.setName("Producer for " + dest3);
        producerThread3.start();

        // Try to consume from queue2, it will make MemoryPercentUsage exceed 70%
        ExecutorService consumerThread = Executors.newSingleThreadExecutor();
        Future<Boolean> result = consumerThread.submit(new Consumer(dest2, 30, false, 5*1000));

        // Try to consumer from queue3, it will not get any messages
        Thread.sleep(500);
        ExecutorService consumerThread2 = Executors.newSingleThreadExecutor();
        Future<Boolean> result2 = consumerThread2.submit(new Consumer(dest3, 30, false, 0));

        producerThread2.join();
        producerThread3.join();
        Assert.assertTrue("Consumer starved. No message received", result.get());

        Thread.sleep(10000);
        // Consume a single from queue1, it will make MemoryPercentUsage < 70% 
        // and allow the consumer for 'Dest3-fast-consumer' to finally consume
        // messages.
        LOG.info("Going to consume one message from Dest1-no-consumer.");
        ExecutorService consumerThread3 = Executors.newSingleThreadExecutor();
        Future<Boolean> result3 = consumerThread.submit(new Consumer(dest1, 1, false, 0));
        Assert.assertTrue("Consumer for " + " dest1 starved. No message received", result2.get());
        Assert.assertTrue("Consumer starved. No message received", result2.get()); //20, TimeUnit.SECONDS));
    }


    /**
     * Retrieves brokers MemoryPercentUsage count from JMX
     * @return MemoryPercentUsage count
     * @throws Exception
     */
    protected static long getQueueSize(String queueName) throws Exception {
        JMXConnector jmxc = null;
        try {
            JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:1099/jmxrmi");
            jmxc = JMXConnectorFactory.connect(url, null);
            MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
            Object tmp = mbsc.getAttribute(new ObjectName("org.apache.activemq:type=Broker,brokerName=localhost" +
                ",destinationType=Queue,destinationName=" +
                queueName
            ), "QueueSize");
            int queueSize = ((Long) tmp).intValue();
            return queueSize;
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw ex;
        } finally {
            if (jmxc != null) 
                jmxc.close();
        }
    }
}

