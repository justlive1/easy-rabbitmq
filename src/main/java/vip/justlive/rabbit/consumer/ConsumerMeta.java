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

package vip.justlive.rabbit.consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * 消费者元信息
 *
 * @author wubo
 */
@Data
public class ConsumerMeta {

  private static final Map<String, ConsumerMeta> METAS = new HashMap<>(4);

  private final String queueName;
  private final String exchangeName;
  private final String exchangeType;
  private final String routing;
  private final String messageConverter;
  private final String group;
  private final String className;


  public static void regist(ConsumerMeta meta) {
    METAS.put(meta.className, meta);
  }

  public static ConsumerMeta lookup(Class<?> type) {
    return METAS.get(type.getName());
  }

  public static Map<String, List<ConsumerMeta>> group() {
    Map<String, List<ConsumerMeta>> groups = new HashMap<>(4);

    for (Map.Entry<String, ConsumerMeta> entry : METAS.entrySet()) {
      groups.computeIfAbsent(entry.getValue().group, k -> new ArrayList<>()).add(entry.getValue());
    }

    return groups;
  }
}
