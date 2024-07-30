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

import java.util.Map;
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
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
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

    if (props.getSources() == null) {
      log.info("'easy-boot.rabbit.sources' is null, easy rabbit is disabled.");
      return;
    }

    if (!props.getSources().containsKey(EasyRabbitProperties.PRIMARY)) {
      log.warn("'easy-boot.rabbit.sources' cannot be without primary source.");
      throw new IllegalArgumentException(
          "'easy-boot.rabbit.sources' cannot be without primary source.");
    }

    CustomMessageConverter converter = new CustomMessageConverter();
    beanFactory.registerSingleton("customMessageConverter", converter);

    for (Map.Entry<String, RabbitProperties> entry : props.getSources().entrySet()) {
      processRabbitAutoConfiguration(entry.getKey(), entry.getValue(), converter, beanFactory);
    }
  }

  private void processRabbitAutoConfiguration(String sourceName, RabbitProperties properties,
      CustomMessageConverter converter, ConfigurableListableBeanFactory beanFactory) {
    RabbitMeta rabbitMeta = new RabbitMeta();
    rabbitMeta.setConverter(converter);

    String suffix = getSuffix(sourceName);
    RabbitConnectionFactoryBeanConfigurer rabbitConnectionFactoryBeanConfigurer = new RabbitConnectionFactoryBeanConfigurer(
        resourceLoader, properties);
    beanFactory.registerSingleton("rabbitConnectionFactoryBeanConfigurer" + suffix,
        rabbitConnectionFactoryBeanConfigurer);

    CachingConnectionFactoryConfigurer cachingConnectionFactoryConfigurer = new CachingConnectionFactoryConfigurer(
        properties);
    beanFactory.registerSingleton("rabbitConnectionFactoryConfigurer" + suffix,
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
    beanFactory.registerSingleton("rabbitConnectionFactory" + suffix,
        cachingConnectionFactory);
    rabbitMeta.setConnectionFactory(cachingConnectionFactory);

    RabbitTemplateConfigurer rabbitTemplateConfigurer = new RabbitTemplateConfigurer(properties);
    rabbitTemplateConfigurer.setMessageConverter(converter);
    beanFactory.registerSingleton("rabbitTemplateConfigurer" + suffix,
        rabbitTemplateConfigurer);

    RabbitTemplate rabbitTemplate = new RabbitTemplate();
    rabbitTemplateConfigurer.configure(rabbitTemplate, cachingConnectionFactory);
    beanFactory.registerSingleton("rabbitTemplate" + suffix, rabbitTemplate);
    rabbitMeta.setRabbitTemplate(rabbitTemplate);

    RabbitAdmin rabbitAdmin = new RabbitAdmin(cachingConnectionFactory);
    beanFactory.registerSingleton("amqpAdmin" + suffix, rabbitAdmin);
    rabbitMeta.setRabbitAdmin(rabbitAdmin);

    RabbitMessagingTemplate rabbitMessagingTemplate = new RabbitMessagingTemplate(rabbitTemplate);
    beanFactory.registerSingleton("rabbitMessagingTemplate" + suffix,
        rabbitMessagingTemplate);
    rabbitMeta.setRabbitMessagingTemplate(rabbitMessagingTemplate);

    RabbitMeta.regist(sourceName, rabbitMeta);
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  private String getSuffix(String source) {
    if (EasyRabbitProperties.PRIMARY.equals(source)) {
      return "";
    }
    return "_" + source;
  }
}
