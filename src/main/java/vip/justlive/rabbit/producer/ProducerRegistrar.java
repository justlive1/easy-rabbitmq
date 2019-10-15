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

package vip.justlive.rabbit.producer;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import vip.justlive.rabbit.annotation.RqueueScan;

/**
 * 生产端注册bean
 *
 * @author wubo
 */
@Slf4j
public class ProducerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware {

  private ResourceLoader resourceLoader;

  @Override
  public void registerBeanDefinitions(AnnotationMetadata metadata,
      BeanDefinitionRegistry registry) {

    AnnotationAttributes attributes =
        AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(RqueueScan.class.getName()));
    if (attributes == null) {
      log.warn("No @RqueueScan was found");
      return;
    }

    String[] basePackages = attributes.getStringArray("value");
    String basePackage = metadata.getClassName()
        .substring(0, metadata.getClassName().lastIndexOf("."));
    if (basePackages.length == 0) {
      basePackages = new String[]{basePackage};
    } else {
      basePackages = Arrays.copyOf(basePackages, basePackages.length + 1);
      basePackages[basePackages.length - 1] = basePackage;
    }

    ProducerScanner scanner = new ProducerScanner(registry);
    scanner.setResourceLoader(resourceLoader);
    scanner.scan(basePackages);
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }
}
