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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.stereotype.Component;
import vip.justlive.rabbit.EasyRabbitProperties;

/**
 * Rqueue
 *
 * @author wubo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Rqueue {

  /**
   * 队列名称
   *
   * @return queue
   */
  String queue();

  /**
   * 交换空间名称
   *
   * @return exchange
   */
  String exchange() default "";

  /**
   * 交换空间工作模式
   *
   * @return type
   */
  String exchangeType() default "direct";

  /**
   * 队列路由
   *
   * @return routing
   */
  String routing() default "";

  /**
   * 消息转换器（BeanName）
   *
   * @return messageConverter
   */
  String messageConverter() default "";

  /**
   * 消息处理分组
   *
   * @return group
   */
  String group() default "";

  /**
   * 数据源，默认使用primary数据源
   *
   * @return datasource
   */
  String datasource() default EasyRabbitProperties.PRIMARY;


}
