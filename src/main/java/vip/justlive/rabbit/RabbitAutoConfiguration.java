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

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;
import vip.justlive.rabbit.annotation.Rqueue;
import vip.justlive.rabbit.consumer.Consumer;
import vip.justlive.rabbit.consumer.ConsumerDef;
import vip.justlive.rabbit.consumer.Receiver;
import vip.justlive.rabbit.converter.CustomMessageConverter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
  public static class ConsumerConfiguration implements EnvironmentAware {
    
    private Environment environment;
    
    @Bean
    public Receiver simpleMessageReceiver() {
      return new Receiver();
    }
    
    @Bean
    @ConditionalOnBean({ConnectionFactory.class, AmqpAdmin.class})
    @ConfigurationProperties(prefix = "spring.rabbitmq.listener.simple")
    public SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory,
                                                                         AmqpAdmin amqpAdmin, Receiver receiver,
                                                                         @Autowired(required = false) List<Consumer<?>> list) {
      Set<String> queueNames = new HashSet<>();
      SimpleMessageListenerContainer container =
          new SimpleMessageListenerContainer(connectionFactory);
      init(list, amqpAdmin, queueNames);
      container.setMessageListener(new MessageListenerAdapter(receiver));
      container.setQueueNames(queueNames.toArray(new String[0]));
      container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
      return container;
    }
    
    private void init(List<Consumer<?>> list, AmqpAdmin amqpAdmin, Set<String> queueNames) {
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
        
        String queueName = environment.resolvePlaceholders(rqueue.queue());
        String exchangeName = environment.resolvePlaceholders(rqueue.exchange());
        String exchangeType = environment.resolvePlaceholders(rqueue.exchangeType());
        String routing = environment.resolvePlaceholders(rqueue.routing());
        String messageConverter = environment.resolvePlaceholders(rqueue.messageConverter());
        
        Queue queue = new Queue(queueName);
        amqpAdmin.declareQueue(queue);
        queueNames.add(queueName);
        
        if (StringUtils.hasText(exchangeName)) {
          Exchange exchange = new ExchangeBuilder(exchangeName, exchangeType).build();
          amqpAdmin.declareExchange(exchange);
          amqpAdmin.declareBinding(
              BindingBuilder.bind(queue).to(exchange).with(routing).noargs());
        }
        log.info("register consumer for [{}][{}][{}] using [{}][{}]", queueName, exchangeName, routing,
            messageConverter, consumer);
        ConsumerDef.register(queueName, exchangeName, routing, messageConverter, consumer);
      }
    }
    
    @Override
    public void setEnvironment(Environment environment) {
      this.environment = environment;
    }
  }
}
