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

package vip.justlive.rabbit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import vip.justlive.rabbit.annotation.Rqueue;
import vip.justlive.rabbit.consumer.Consumer;
import vip.justlive.rabbit.consumer.ConsumerDef;
import vip.justlive.rabbit.consumer.Receiver;
import vip.justlive.rabbit.converter.CustomMessageConverter;

/**
 * rabbit auto configuration
 *
 * @author wubo
 */
@Slf4j
@Configuration
public class RabbitAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(MessageConverter.class)
  public MessageConverter messageConverter() {
    return new CustomMessageConverter();
  }

  @Configuration
  @ConditionalOnProperty(name = "spring.rabbitmq.listener.enabled", havingValue = "true")
  public class ConsumerConfiguration {

    private Set<String> queueNames = new HashSet<>();

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired(required = false)
    private List<Consumer<?>> list;

    @Value("${spring.rabbitmq.listener.prefetchCount:1}")
    private int prefetchCount;

    @Value("${spring.rabbitmq.listener.txSize:1}")
    private int txSize;

    @Bean
    public SimpleMessageListenerContainer simpleMessageListenerContainer(MessageConverter converter,
        ConnectionFactory connectionFactory) {
      SimpleMessageListenerContainer container =
          new SimpleMessageListenerContainer(connectionFactory);
      init(list);
      Receiver receiver = new Receiver(converter);
      container.setMessageListener(new MessageListenerAdapter(receiver, converter));
      container.setQueueNames(queueNames.toArray(new String[0]));
      container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
      container.setPrefetchCount(prefetchCount);
      container.setTxSize(txSize);
      return container;
    }

    public void init(List<Consumer<?>> list) {
      if (list == null || list.isEmpty()) {
        log.warn("not found MessageProcess");
        return;
      }

      for (Consumer<?> consumer : list) {
        Rqueue rqueue = consumer.getClass().getAnnotation(Rqueue.class);
        if (rqueue == null) {
          log.warn("{} should be annotated by @Rqueue", consumer);
          continue;
        }

        Queue queue = new Queue(rqueue.queue());
        amqpAdmin.declareQueue(queue);
        queueNames.add(rqueue.queue());

        if (StringUtils.hasText(rqueue.exchange())) {
          Exchange exchange = new ExchangeBuilder(rqueue.exchange(), rqueue.exchangeType()).build();
          amqpAdmin.declareExchange(exchange);
          amqpAdmin.declareBinding(
              BindingBuilder.bind(queue).to(exchange).with(rqueue.routing()).noargs());
        }
        ConsumerDef.register(rqueue.queue(), rqueue.exchange(), rqueue.routing(), consumer);
      }
    }
  }
}
