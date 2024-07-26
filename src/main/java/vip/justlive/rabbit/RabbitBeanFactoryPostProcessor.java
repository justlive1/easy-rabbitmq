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

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.CachingConnectionFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitConnectionFactoryBeanConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import vip.justlive.rabbit.converter.CustomMessageConverter;

/**
 * rabbit动态注册bean
 *
 * @author wubo
 */
@Slf4j
public class RabbitBeanFactoryPostProcessor implements BeanFactoryPostProcessor, EnvironmentAware,
    ResourceLoaderAware {

  private Environment environment;
  private ResourceLoader resourceLoader;

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

    processRabbitAutoConfiguration(props, beanFactory);
  }

  private void processRabbitAutoConfiguration(EasyRabbitProperties properties,
      ConfigurableListableBeanFactory beanFactory) {
    RabbitMeta rabbitMeta = new RabbitMeta();

    RabbitConnectionFactoryBeanConfigurer rabbitConnectionFactoryBeanConfigurer = new RabbitConnectionFactoryBeanConfigurer(
        resourceLoader,
        properties.getConfig());
    beanFactory.registerSingleton("rabbitConnectionFactoryBeanConfigurer",
        rabbitConnectionFactoryBeanConfigurer);

    CachingConnectionFactoryConfigurer cachingConnectionFactoryConfigurer = new CachingConnectionFactoryConfigurer(
        properties.getConfig());
    beanFactory.registerSingleton("rabbitConnectionFactoryConfigurer",
        cachingConnectionFactoryConfigurer);

    RabbitConnectionFactoryBean connectionFactoryBean = new RabbitConnectionFactoryBean();
    rabbitConnectionFactoryBeanConfigurer.configure(connectionFactoryBean);
    connectionFactoryBean.afterPropertiesSet();

    com.rabbitmq.client.ConnectionFactory connectionFactory;
    try {
      connectionFactory = connectionFactoryBean.getObject();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (connectionFactory == null) {
      throw new RuntimeException("connectionFactory getObject is null");
    }
    CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(
        connectionFactory);
    cachingConnectionFactoryConfigurer.configure(cachingConnectionFactory);
    beanFactory.registerSingleton("rabbitConnectionFactory", cachingConnectionFactory);
    rabbitMeta.setConnectionFactory(cachingConnectionFactory);

    CustomMessageConverter converter = new CustomMessageConverter();
    beanFactory.registerSingleton("customMessageConverter", converter);
    rabbitMeta.setConverter(converter);

    RabbitTemplateConfigurer rabbitTemplateConfigurer = new RabbitTemplateConfigurer(
        properties.getConfig());
    rabbitTemplateConfigurer.setMessageConverter(converter);
    beanFactory.registerSingleton("rabbitTemplateConfigurer", rabbitTemplateConfigurer);

    RabbitTemplate rabbitTemplate = new RabbitTemplate();
    rabbitTemplateConfigurer.configure(rabbitTemplate, cachingConnectionFactory);
    beanFactory.registerSingleton("rabbitTemplate", rabbitTemplate);
    rabbitMeta.setRabbitTemplate(rabbitTemplate);

    RabbitAdmin rabbitAdmin = new RabbitAdmin(cachingConnectionFactory);
    beanFactory.registerSingleton("amqpAdmin", rabbitAdmin);
    rabbitMeta.setRabbitAdmin(rabbitAdmin);

    RabbitMessagingTemplate rabbitMessagingTemplate = new RabbitMessagingTemplate(rabbitTemplate);
    beanFactory.registerSingleton("rabbitMessagingTemplate", rabbitMessagingTemplate);
    rabbitMeta.setRabbitMessagingTemplate(rabbitMessagingTemplate);

    RabbitMeta.regist(EasyRabbitProperties.PRIMARY, rabbitMeta);
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }
}
