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
package org.apache.activemq.journal;


import org.apache.activemq.api.core.ActiveMQIOErrorException;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;

/**
 * @author <a href="mailto:andy.taylor@jboss.org">Andy Taylor</a>
 *         3/12/12
 *
 * Logger Code 14
 *
 * each message id must be 6 digits long starting with 14, the 3rd digit should be 9
 *
 * so 149000 to 149999
 */
@MessageBundle(projectCode = "AMQ")
public interface ActiveMQJournalBundle
{
   ActiveMQJournalBundle BUNDLE = Messages.getBundle(ActiveMQJournalBundle.class);

   @Message(id = 149000, value =  "failed to rename file {0} to {1}", format = Message.Format.MESSAGE_FORMAT)
   ActiveMQIOErrorException ioRenameFileError(String name, String newFileName);

   @Message(id = 149001, value =  "Journal data belong to a different version", format = Message.Format.MESSAGE_FORMAT)
   ActiveMQIOErrorException journalDifferentVersion();

   @Message(id = 149002, value =  "Journal files version mismatch. You should export the data from the previous version and import it as explained on the user''s manual",
         format = Message.Format.MESSAGE_FORMAT)
   ActiveMQIOErrorException journalFileMisMatch();

   @Message(id = 149003, value =   "File not opened", format = Message.Format.MESSAGE_FORMAT)
   ActiveMQIOErrorException fileNotOpened();
}
