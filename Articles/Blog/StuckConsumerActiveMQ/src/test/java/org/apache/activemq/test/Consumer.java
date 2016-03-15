package org.apache.activemq.test;

import java.util.concurrent.Callable;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * General purpose message consumer running as a 
 * separate thread, connecting to broker.
 */
public class Consumer implements Callable {

    public Object init = new Object();
    protected String queueName = "";
    protected int numMsgs = 1;
    protected boolean isTopic = false;
    protected int sleepTime = 0;

    Logger log = null;

    public Consumer(String destName, int numMsgs, boolean topic, int sleepTime) {
        log = LoggerFactory.getLogger(this.getClass());
        this.isTopic = topic;
        this.numMsgs = numMsgs;
        this.queueName = destName;
        this.sleepTime = sleepTime;
    }

    /**
     * connect to broker and receive messages
     */
    public Boolean call() {
        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;

        try {
            ActiveMQConnectionFactory amq = new ActiveMQConnectionFactory(AMQTemplateTest.BROKERURL);
            connection = amq.createConnection();

            connection.setExceptionListener(new javax.jms.ExceptionListener() {
                    public void onException(javax.jms.JMSException e) {
                        e.printStackTrace();
                    }
            });
            connection.start();

            // Create a Session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Create the destination (Topic or Queue)
            Destination  destination = null;
            if (isTopic) 
                destination = session.createTopic(queueName);
            else 
                destination = session.createQueue(queueName);

            consumer = session.createConsumer(destination);

            log.info("Consumer entering wait loop for messages");
            for (int i = 0; i < numMsgs; i++) {
                Message message = consumer.receive(3*1000);

                if (message == null) {
                    log.warn("I got starved and did not receive a message " + 
                        "for queue {} despite its queue size being {}",
                        queueName,
                        AMQTemplateTest.getQueueSize(queueName));
                    i--;
                }
                else if (message instanceof TextMessage) {
                    // TextMessage textMessage = (TextMessage) message;
                    // String text = textMessage.getText().substring(0, 10);
                    log.debug("Received message {} for {}", i, queueName);
                } else {
                    log.warn("Received message of unsupported type. Expecting TextMessage. " + message);
                }
                Thread.sleep(sleepTime);
            }
            return new Boolean(true);
        } catch(Exception e) {
            log.error("Error in Consumer: " + e.getMessage());
            return new Boolean(false);
        } finally {
            try {
                if (consumer != null)
                    consumer.close();
                if (session != null)
                    session.close();
                if (connection != null)
                    connection.close();
                log.info("Consumer for dest {} finished.", queueName);
            } catch (Exception ex) {
                log.error("Error closing down JMS objects: " + ex.getMessage());
            }
        }
    }
}
