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
package org.apache.activemq.byteman.tests;

import java.util.concurrent.CountDownLatch;

import org.apache.activemq.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.api.core.TransportConfiguration;
import org.apache.activemq.core.config.Configuration;
import org.apache.activemq.core.server.ActiveMQServer;
import org.apache.activemq.tests.util.ReplicatedBackupUtils;
import org.apache.activemq.tests.util.ServiceTestBase;
import org.apache.activemq.tests.util.TransportConfigurationUtils;
import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMRules;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BMUnitRunner.class)
public class ReplicationBackupTest extends ServiceTestBase
{
   private static final CountDownLatch ruleFired = new CountDownLatch(1);
   private ActiveMQServer backupServer;
   private ActiveMQServer liveServer;

   /*
   * simple test to induce a potential race condition where the server's acceptors are active, but the server's
   * state != STARTED
   */
   @Test
   @BMRules
      (
         rules =
            {
               @BMRule
                  (
                     name = "prevent backup annoucement",
                     targetClass = "org.apache.activemq.core.server.impl.SharedNothingLiveActivation",
                     targetMethod = "run",
                     targetLocation = "AT EXIT",
                     action = "org.apache.activemq.byteman.tests.ReplicationBackupTest.breakIt();"
                  )
            }
      )
   public void testReplicatedBackupAnnouncement() throws Exception
   {
      TransportConfiguration liveConnector = TransportConfigurationUtils.getNettyConnector(true, 0);
      TransportConfiguration liveAcceptor = TransportConfigurationUtils.getNettyAcceptor(true, 0);
      TransportConfiguration backupConnector = TransportConfigurationUtils.getNettyConnector(false, 0);
      TransportConfiguration backupAcceptor = TransportConfigurationUtils.getNettyAcceptor(false, 0);

      final String suffix = "_backup";

      Configuration backupConfig = createDefaultConfig()
         .setBindingsDirectory(ActiveMQDefaultConfiguration.getDefaultBindingsDirectory() + suffix)
         .setJournalDirectory(ActiveMQDefaultConfiguration.getDefaultJournalDir() + suffix)
         .setPagingDirectory(ActiveMQDefaultConfiguration.getDefaultPagingDir() + suffix)
         .setLargeMessagesDirectory(ActiveMQDefaultConfiguration.getDefaultLargeMessagesDir() + suffix);

      Configuration liveConfig = createDefaultConfig();

      ReplicatedBackupUtils.configureReplicationPair(backupConfig, backupConnector, backupAcceptor, liveConfig, liveConnector, liveAcceptor);

      liveServer = createServer(liveConfig);

      // start the live server in a new thread so we can start the backup simultaneously to induce a potential race
      Thread startThread = new Thread(new Runnable()
      {
         @Override
         public void run()
         {
            try
            {
               liveServer.start();
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
         }
      });
      startThread.start();

      ruleFired.await();

      backupServer = createServer(backupConfig);
      backupServer.start();
      ServiceTestBase.waitForRemoteBackup(null, 3, true, backupServer);
   }

   public static void breakIt()
   {
      ruleFired.countDown();
      try
      {
         /* before the fix this sleep would put the "live" server into a state where the acceptors were started
          * but the server's state != STARTED which would cause the backup to fail to announce
          */
         Thread.sleep(2000);
      }
      catch (InterruptedException e)
      {
         e.printStackTrace();
      }
   }
}