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

package vip.justlive.rabbit;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import vip.justlive.rabbit.annotation.Rqueue;
import vip.justlive.rabbit.consumer.Consumer;
import vip.justlive.rabbit.consumer.ConsumerMeta;
import vip.justlive.rabbit.consumer.Receiver;

/**
 * rabbit消费者动态注册bean
 *
 * @author wubo
 */
@Slf4j
public class RabbitConsumerBeanFactoryPostProcessor implements BeanFactoryPostProcessor,
    EnvironmentAware {

  private Environment environment;


  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
      throws BeansException {

    Binder binder = Binder.get(environment);
    EasyRabbitProperties props = binder.bindOrCreate(EasyRabbitProperties.PREFIX,
        EasyRabbitProperties.class);

    if (props.getConfig() == null) {
      log.info("'easy-boot.rabbit.config' is null, easy rabbit is disabled.");
      return;
    }
    if (!props.getConsumer().isEnabled()) {
      log.info(
          "'easy-boot.rabbit.consumer.enabled' is not true, easy rabbit consumer is disabled.");
      return;
    }

    processRabbitConsumerMeta(beanFactory);
    processRabbitAutoConfiguration(props, beanFactory);
  }


  private void processRabbitConsumerMeta(ConfigurableListableBeanFactory beanFactory) {

    for (String name : beanFactory.getBeanNamesForType(Consumer.class)) {
      String className = beanFactory.getBeanDefinition(name).getBeanClassName();
      Rqueue rqueue;
      try {
        rqueue = Class.forName(className).getAnnotation(Rqueue.class);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
      if (rqueue == null) {
        log.warn("{} should be annotated by @Rqueue", className);
        continue;
      }

      String queueName = environment.resolvePlaceholders(rqueue.queue());
      String exchangeName = environment.resolvePlaceholders(rqueue.exchange());
      String exchangeType = environment.resolvePlaceholders(rqueue.exchangeType());
      String routing = environment.resolvePlaceholders(rqueue.routing());
      String messageConverter = environment.resolvePlaceholders(rqueue.messageConverter());
      String group = environment.resolvePlaceholders(rqueue.group());

      ConsumerMeta meta = new ConsumerMeta(queueName, exchangeName, exchangeType, routing,
          messageConverter, group, className);
      ConsumerMeta.regist(meta);

      log.info("find Rqueue meta: {} ", meta);
    }
  }

  private void processRabbitAutoConfiguration(EasyRabbitProperties props,
      ConfigurableListableBeanFactory beanFactory) {

    RabbitMeta rabbitMeta = RabbitMeta.lookup(EasyRabbitProperties.PRIMARY);
    if (rabbitMeta == null) {
      log.error("Primary RabbitMeta not found.");
      return;
    }

    Receiver receiver = new Receiver(rabbitMeta.getConverter(), props);
    beanFactory.registerSingleton("easyRabbitReceiver", receiver);

    Map<String, List<ConsumerMeta>> groups = ConsumerMeta.group();
    for (Map.Entry<String, List<ConsumerMeta>> entry : groups.entrySet()) {
      SimpleMessageListenerContainer container = simpleMessageListenerContainer(props,
          rabbitMeta.getConnectionFactory(), receiver, entry.getValue());
      String beanName = String.format("simpleMessageListenerContainer_%s", entry.getKey());
      container.setBeanName(beanName);
      beanFactory.registerSingleton(beanName, container);
    }

  }


  public SimpleMessageListenerContainer simpleMessageListenerContainer(
      EasyRabbitProperties properties, ConnectionFactory connectionFactory, Receiver receiver,
      List<ConsumerMeta> consumerMetas) {

    SimpleRabbitListenerContainerFactoryConfigurer configurer = new SimpleRabbitListenerContainerFactoryConfigurer(
        properties.getConfig());
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connectionFactory);
    SimpleMessageListenerContainer container = factory.createListenerContainer();

    Set<String> queueNames = consumerMetas.stream().map(ConsumerMeta::getQueueName)
        .collect(Collectors.toSet());

    container.setMessageListener(new MessageListenerAdapter(receiver));
    container.setQueueNames(queueNames.toArray(new String[0]));
    container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

    log.info("init message listener for queue(s) {}", queueNames);
    return container;
  }
}
