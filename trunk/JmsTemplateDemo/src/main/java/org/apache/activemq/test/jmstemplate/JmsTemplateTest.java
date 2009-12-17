package org.apache.activemq.test.jmstemplate;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;


public class JmsTemplateTest {

    public static void main(String[] args) throws Exception {

        Logger logger = Logger.getLogger(JmsTemplateTest.class);

        ApplicationContext ctx = new ClassPathXmlApplicationContext(
          "JmsTemplateTest-context.xml");
        JmsTemplate template = (JmsTemplate) ctx.getBean("jmsTemplate");
        
        for(int i=0; i<10; i++) {
            template.send("MyQueue", new MessageCreator() {
                public Message createMessage(Session session)
                    throws JMSException {
                    TextMessage tm = session.createTextMessage();
                    tm.setText("This is a test message");
                    return tm;
                }
            });
            logger.info("Message sent");
        }
        System.exit(1);
    }
}
