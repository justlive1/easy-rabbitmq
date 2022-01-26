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

package vip.justlive.rabbit.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.env.Environment;
import vip.justlive.rabbit.annotation.Rqueue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * proxy
 *
 * @param <T> 泛型
 * @author wubo
 */
@Slf4j
public class ProducerProxy<T> implements InvocationHandler {
  
  private final boolean exchangeMode;
  private final RabbitTemplate template;
  private final QueueProperties queueProperties;
  
  ProducerProxy(Class<T> clazz, RabbitTemplate template, Environment environment) {
    Rqueue rqueue = clazz.getAnnotation(Rqueue.class);
    if (rqueue == null) {
      throw new IllegalArgumentException("BaseProducer 接口需要 @Rqueue");
    }
    
    String queue = environment.resolvePlaceholders(rqueue.queue());
    String exchange = environment.resolvePlaceholders(rqueue.exchange());
    String routing = environment.resolvePlaceholders(rqueue.routing());
    String messageConverter = environment.resolvePlaceholders(rqueue.messageConverter());
    this.queueProperties = new QueueProperties(queue, exchange, routing, messageConverter);
    this.exchangeMode = exchange.length() > 0;
    this.template = template;
    
    log.info("created producer proxy for queue [{}][{}][{}]", queue, exchange, routing);
  }
  
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    
    if (!BaseProducer.class.equals(method.getDeclaringClass())) {
      return method.invoke(this, args);
    }
    
    QueueProperties.set(queueProperties);
    
    if (exchangeMode) {
      template.convertAndSend(queueProperties.getExchange(), queueProperties.getRouting(), args[0], queueProperties);
    } else {
      template.convertAndSend(queueProperties.getRouting(), args[0], queueProperties);
    }
    return null;
  }
  
}
