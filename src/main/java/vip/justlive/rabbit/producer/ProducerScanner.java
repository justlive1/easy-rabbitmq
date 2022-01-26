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
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import vip.justlive.rabbit.annotation.Rqueue;

import java.util.Arrays;
import java.util.Set;

/**
 * 生产端扫描器
 *
 * @author wubo
 */
@Slf4j
public class ProducerScanner extends ClassPathBeanDefinitionScanner {
  
  ProducerScanner(BeanDefinitionRegistry registry) {
    super(registry, false);
    addIncludeFilter(new AnnotationTypeFilter(Rqueue.class));
  }
  
  @Override
  protected Set<BeanDefinitionHolder> doScan(String... basePackages) {
    Set<BeanDefinitionHolder> beanDefinitions = super.doScan(basePackages);
    if (beanDefinitions.isEmpty()) {
      log.warn("No Producer was found in '{}' package. Please check your configuration.",
          Arrays.toString(basePackages));
    } else {
      processBeanDefinitions(beanDefinitions);
    }
    return beanDefinitions;
  }
  
  @Override
  protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
    return beanDefinition.getMetadata().isInterface()
        && beanDefinition.getMetadata().isIndependent();
  }
  
  private void processBeanDefinitions(Set<BeanDefinitionHolder> beanDefinitions) {
    GenericBeanDefinition definition;
    
    for (BeanDefinitionHolder holder : beanDefinitions) {
      definition = (GenericBeanDefinition) holder.getBeanDefinition();
      
      String beanClassName = definition.getBeanClassName();
      if (beanClassName == null) {
        continue;
      }
      if (log.isDebugEnabled()) {
        log.debug("creating ProducerFactoryBean with name '{}' and '{}' interface",
            holder.getBeanName(), beanClassName);
      }
      
      definition.getConstructorArgumentValues().addGenericArgumentValue(beanClassName);
      definition.setBeanClass(ProducerFactoryBean.class);
      definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    }
  }
}
