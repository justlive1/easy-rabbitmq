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

import lombok.Data;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.util.StringUtils;
import vip.justlive.rabbit.EasyRabbitProperties;

/**
 * 队列相关属性
 *
 * @author wubo
 */
@Data
public class QueueProperties implements MessagePostProcessor {

  private static final ThreadLocal<QueueProperties> HOLDER = new ThreadLocal<>();

  private final String queue;
  private final String exchange;
  private final String routing;
  private final String messageConverter;

  public static void set(QueueProperties queueProperties) {
    HOLDER.set(queueProperties);
  }

  public static QueueProperties get() {
    return HOLDER.get();
  }

  @Override
  public Message postProcessMessage(Message message) {

    String traceIdKey = ProducerRegistryPostProcessor.CTX.get().getBean(EasyRabbitProperties.class)
        .getTraceIdKey();
    if (StringUtils.hasText(traceIdKey)) {
      String traceId = MDC.get(traceIdKey);
      if (StringUtils.hasText(traceId)) {
        message.getMessageProperties().setHeader(traceIdKey, traceId);
      }
    }

    HOLDER.remove();
    return message;
  }

}
