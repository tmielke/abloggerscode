
DESCRIPTION:
==============
This is a simple Maven based demo that illustrates how to use the JmsTemplate so that JMS resources get pooleed. When using Springs JmsTemplate in a standalone Java application (outside of an application server container) it is important to correctly configure the JMS environment to pool resources. 
See http://tmielke.blogspot.com/ for more information on this subject.


COMPILING:
===========
Simply run mvn install or import the project into Eclipse.


RUNNING:
==========
- start ActiveMQ
- run mvn exec:java, it will by default connect to URL
  tcp://localhost:61616



OUTPUT:
=======
$ mvn exec:java
[INFO] Scanning for projects...
[INFO] Searching repository for plugin with prefix: 'exec'.
[INFO] ------------------------------------------------------------------------
[INFO] Building Testcase showing how to use the JmsTemplate and pool JMS resources
[INFO]    task-segment: [exec:java]
[INFO] ------------------------------------------------------------------------
[INFO] Preparing exec:java
[INFO] No goals needed for project - skipping
[INFO] [exec:java]
2009-12-17 16:16:14,928 ClassPathXmlApplicationContext INFO  Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@10c3a08: display name [org.springframework.context.support.ClassPathXmlApplicationContext@10c3a08]; startup date [Thu Dec 17 16:16:14 CET 2009]; root of context hierarchy
2009-12-17 16:16:14,990 XmlBeanDefinitionReader        INFO  Loading XML bean definitions from class path resource [JmsTemplateTest-context.xml]
2009-12-17 16:16:15,146 ClassPathXmlApplicationContext INFO  Bean factory for application context [org.springframework.context.support.ClassPathXmlApplicationContext@10c3a08]: org.springframework.beans.factory.support.DefaultListableBeanFactory@10d0eae
2009-12-17 16:16:15,177 DefaultListableBeanFactory     INFO  Pre-instantiating singletons in org.springframework.beans.factory.support.DefaultListableBeanFactory@10d0eae: defining beans [connectionFactory,jmsTemplate]; root of factory hierarchy
2009-12-17 16:16:15,519 FailoverTransport              INFO  Successfully connected to tcp://localhost:61616
2009-12-17 16:16:15,612 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent
2009-12-17 16:16:15,628 JmsTemplateTest                INFO  Message sent


