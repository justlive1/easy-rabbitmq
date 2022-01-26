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

package vip.justlive.rabbit.annotation;

import org.springframework.context.annotation.Import;
import vip.justlive.rabbit.producer.ProducerRegistrar;

import java.lang.annotation.*;


/**
 * Rqueue 扫描 用于发送端
 *
 * @author wubo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ProducerRegistrar.class)
public @interface RqueueScan {
  
  /**
   * 包路径
   *
   * @return packages
   */
  String[] value() default {};
}
