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

package vip.justlive.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import vip.justlive.rabbit.producer.ProducerRegistryPostProcessor;

/**
 * rabbit auto configuration
 *
 * @author wubo
 */
@Slf4j
@EnableConfigurationProperties(EasyRabbitProperties.class)
@ConditionalOnProperty(name = "easy-boot.rabbit.producer.enabled", havingValue = "true")
@AutoConfigureBefore(RabbitAutoConfiguration.class)
public class RabbitProducerAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public RabbitBeanFactoryPostProcessor rabbitBeanFactoryPostProcessor() {
    return new RabbitBeanFactoryPostProcessor();
  }

  @Bean
  @ConditionalOnMissingBean
  public ProducerRegistryPostProcessor producerRegistryPostProcessor() {
    return new ProducerRegistryPostProcessor();
  }


}
