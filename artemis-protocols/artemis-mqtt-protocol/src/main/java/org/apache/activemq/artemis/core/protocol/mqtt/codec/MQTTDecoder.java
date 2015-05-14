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

package org.apache.activemq.artemis.core.protocol.mqtt.codec;

import java.util.ArrayList;
import java.util.List;

import io.netty.handler.codec.mqtt.MqttDecoder;
import org.apache.activemq.artemis.api.core.ActiveMQBuffer;

public class MQTTDecoder extends MqttDecoder
{
   public MQTTDecoder(int maxMessageLength)
   {
      super(maxMessageLength);
   }

   public MQTTDecoder()
   {
      super();
   }

   public List<Object> decode(ActiveMQBuffer buffer) throws Exception
   {
      List<Object> messages = new ArrayList<>();
      decode(null, buffer.byteBuf(), messages);
      return messages;
   }
}
