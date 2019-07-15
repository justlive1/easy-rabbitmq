/*
 * Copyright (C) 2019 justlive1
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License
 *  is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing permissions and limitations under
 *  the License.
 */

package vip.justlive.rabbit.consumer;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import java.lang.reflect.Type;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import vip.justlive.rabbit.converter.CustomMessageConverter;

/**
 * 接收处理器
 *
 * @author wubo
 */
@Slf4j
public class Receiver implements ChannelAwareMessageListener {

  private final MessageConverter converter;

  public Receiver(MessageConverter converter) {
    this.converter = converter;
  }

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
      if (converter instanceof CustomMessageConverter && msg instanceof byte[]) {
        // 特殊处理
        Type type = consumer.getType();
        if (type != byte[].class) {
          msg = JSON.parseObject((byte[]) msg, type);
        }
      }
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
