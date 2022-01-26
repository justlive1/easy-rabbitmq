/*
 * Copyright (C) 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package vip.justlive.rabbit.consumer;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;

/**
 * 接收处理器
 *
 * @author wubo
 */
@Slf4j
@RequiredArgsConstructor
public class Receiver implements ChannelAwareMessageListener {
  
  private final MessageConverter converter;
  
  @Override
  public void onMessage(Message message, Channel channel) throws Exception {
    try {
      MessageProperties prop = message.getMessageProperties();
      ConsumerDef consumer = ConsumerDef.lookup(prop.getConsumerQueue(), prop.getReceivedExchange(),
          prop.getReceivedRoutingKey());
      if (consumer == null) {
        log.error("consumer not found {}", prop);
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        return;
      }
      
      Object msg = converter.fromMessage(message);
      
      if (log.isDebugEnabled()) {
        log.debug("receive msg {}", msg);
      }
      consumer.accept(msg);
      channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    } catch (Exception e) {
      log.error("receive msg error {}", message, e);
      channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
    }
  }
}
