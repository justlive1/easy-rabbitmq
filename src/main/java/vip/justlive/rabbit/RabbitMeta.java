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

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import vip.justlive.rabbit.converter.CustomMessageConverter;

/**
 * rabbit相关信息
 *
 * @author wubo
 */
@Data
public class RabbitMeta {

  private static final Map<String, RabbitMeta> METAS = new HashMap<>(4);

  private CustomMessageConverter converter;
  private ConnectionFactory connectionFactory;
  private RabbitTemplate rabbitTemplate;
  private AmqpAdmin rabbitAdmin;
  private RabbitMessagingTemplate rabbitMessagingTemplate;

  public static void regist(String source, RabbitMeta meta) {
    METAS.put(source, meta);
  }

  public static RabbitMeta lookup(String source) {
    return METAS.get(source);
  }
}
