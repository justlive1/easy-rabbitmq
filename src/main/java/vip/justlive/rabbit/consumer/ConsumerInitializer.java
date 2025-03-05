/*
 * Copyright (C) 2024 the original author or authors.
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

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.util.StringUtils;
import vip.justlive.rabbit.RabbitMeta;

/**
 * 消费者初始化执行器
 *
 * @author wubo
 */
@Slf4j
@RequiredArgsConstructor
public class ConsumerInitializer {

  private final List<Consumer<?>> consumers;

  @PostConstruct
  public void initialize() {

    if (consumers == null) {
      return;
    }

    Set<String> queueNames = new HashSet<>();

    for (Consumer<?> consumer : consumers) {
      ConsumerMeta meta = ConsumerMeta.lookup(consumer.getClass());
      if (meta == null) {
        continue;
      }

      RabbitMeta rabbitMeta = RabbitMeta.lookup(meta.getDatasource());
      if (rabbitMeta == null) {
        continue;
      }

      AmqpAdmin amqpAdmin = rabbitMeta.getRabbitAdmin();
      Queue queue = new Queue(meta.getQueueName());
      if (queueNames.add(meta.getQueueName())) {
        amqpAdmin.declareQueue(queue);
      }

      if (StringUtils.hasText(meta.getExchangeName())) {
        Exchange exchange = new ExchangeBuilder(meta.getExchangeName(),
            meta.getExchangeType()).build();
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareBinding(
            BindingBuilder.bind(queue).to(exchange).with(meta.getRouting()).noargs());
      }
      log.info("register consumer for [{}][{}][{}] -> [{}] using [{}][{}]", meta.getQueueName(),
          meta.getExchangeName(), meta.getRouting(), meta.getDatasource(),
          meta.getMessageConverter(), consumer);
      ConsumerDef.register(meta.getQueueName(), meta.getExchangeName(), meta.getRouting(),
          meta.getMessageConverter(), consumer);
    }
  }


}
