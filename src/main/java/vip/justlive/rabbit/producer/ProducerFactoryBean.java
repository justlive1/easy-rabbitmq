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

import java.lang.reflect.Proxy;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;


/**
 * 生产端工厂bean
 *
 * @param <T> 泛型
 * @author wubo
 */
public class ProducerFactoryBean<T> implements FactoryBean<T>, EnvironmentAware {

  private final Class<T> clazz;
  private Environment environment;

  public ProducerFactoryBean(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Override
  public T getObject() {
    return clazz.cast(Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz},
        new ProducerProxy<>(clazz, environment)));
  }

  @Override
  public Class<?> getObjectType() {
    return clazz;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}
