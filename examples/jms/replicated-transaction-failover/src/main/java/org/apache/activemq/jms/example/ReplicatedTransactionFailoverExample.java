/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.jms.example;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TransactionRolledBackException;
import javax.naming.InitialContext;

import org.apache.activemq.api.core.Message;
import org.apache.activemq.common.example.ActiveMQExample;

/**
 * A simple example that demonstrates failover of the JMS connection from one node to another
 * when the live server crashes using a JMS <em>transacted</em> session and replication.
 *
 * @author <a href="mailto:jmesnil@redhat.com">Jeff Mesnil</a>
 */
public class ReplicatedTransactionFailoverExample extends ActiveMQExample
{

   // You need to guarantee uniqueIDs when using duplicate detection
   // It needs to be unique even after a restart
   // as these IDs are stored on the journal for control
   // We recommend some sort of UUID, but for this example the Current Time as string would be enough
   String uniqueID = Long.toString(System.currentTimeMillis());

   public static void main(final String[] args)
   {
      new ReplicatedTransactionFailoverExample().run(args);
   }

   @Override
   public boolean runExample() throws Exception
   {
      final int numMessages = 10;

      Connection connection = null;

      InitialContext initialContext = null;

      try
      {
         // Step 1. Get an initial context for looking up JNDI from the server #1
         initialContext = new InitialContext();

         // Step 2. Look-up the JMS resources from JNDI
         Queue queue = (Queue)initialContext.lookup("queue/exampleQueue");
         ConnectionFactory connectionFactory = (ConnectionFactory)initialContext.lookup("ConnectionFactory");

         // Step 3. We create a JMS Connection
         connection = connectionFactory.createConnection();

         // Step 4. We create a *transacted* JMS Session
         Session session = connection.createSession(true, 0);

         // Step 5. We start the connection to ensure delivery occurs
         connection.start();

         // Step 6. We create a JMS MessageProducer
         MessageProducer producer = session.createProducer(queue);

         // Step 7. We create a JMS MessageConsumer
         MessageConsumer consumer = session.createConsumer(queue);

         // Step 8. We send half of the messages, kill the live server and send the remaining messages
         sendMessages(session, producer, numMessages, true);

         // Step 9. As failover occurred during transaction, the session has been marked for rollback only
         try
         {
            session.commit();
         }
         catch (TransactionRolledBackException e)
         {
            System.err.println("transaction has been rolled back: " + e.getMessage());
         }

         // Step 10. We resend all the messages
         sendMessages(session, producer, numMessages, false);

         // Step 11. We commit the session successfully: the messages will be all delivered to the activated backup
         // server
         session.commit();

         // Step 12. We are now transparently reconnected to server #0, the backup server.
         // We consume the messages sent before the crash of the live server and commit the session.
         for (int i = 0; i < numMessages; i++)
         {
            TextMessage message0 = (TextMessage)consumer.receive(5000);

            if (message0 == null)
            {
               System.err.println("Example failed - message wasn't received");

               return false;
            }

            System.out.println("Got message: " + message0.getText());
         }

         session.commit();

         System.out.println("Other message on the server? " + consumer.receive(5000));

         return true;
      }
      finally
      {
         // Step 13. Be sure to close our resources!

         if (connection != null)
         {
            connection.close();
         }

         if (initialContext != null)
         {
            initialContext.close();
         }
      }
   }

   private void sendMessages(final Session session,
                             final MessageProducer producer,
                             final int numMessages,
                             final boolean killServer) throws Exception
   {
      // We send half of messages
      for (int i = 0; i < numMessages / 2; i++)
      {
         TextMessage message = session.createTextMessage("This is text message " + i);

         // We set the duplicate detection header - so the server will ignore the same message
         // if sent again after failover

         message.setStringProperty(Message.HDR_DUPLICATE_DETECTION_ID.toString(), uniqueID + i);

         producer.send(message);

         System.out.println("Sent message: " + message.getText());
      }

      if (killServer)
      {
         Thread.sleep(5000);

         killServer(0);

      }

      // We send the remaining half of messages
      for (int i = numMessages / 2; i < numMessages; i++)
      {
         TextMessage message = session.createTextMessage("This is text message " + i);

         // We set the duplicate detection header - so the server will ignore the same message
         // if sent again after failover

         message.setStringProperty(Message.HDR_DUPLICATE_DETECTION_ID.toString(), uniqueID + i);

         producer.send(message);

         System.out.println("Sent message: " + message.getText());
      }
   }

}
