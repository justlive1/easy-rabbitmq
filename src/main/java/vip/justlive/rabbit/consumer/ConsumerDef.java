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

package vip.justlive.rabbit.consumer;

import lombok.Getter;
import vip.justlive.rabbit.producer.QueueProperties;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * consumer定义类
 *
 * @author wubo
 */
@Getter
public class ConsumerDef implements Consumer<Object> {
  
  private static final Map<String, ConsumerDef> CONSUMERS = new HashMap<>(4);
  
  private final QueueProperties queueProperties;
  private final Consumer<Object> delegate;
  
  private Type type;
  
  @SuppressWarnings("unchecked")
  private ConsumerDef(QueueProperties queueProperties, Consumer<?> delegate) {
    this.queueProperties = queueProperties;
    this.delegate = (Consumer<Object>) delegate;
  }
  
  @Override
  public void accept(Object msg) {
    this.delegate.accept(msg);
  }
  
  public Type getType() {
    if (type != null) {
      return type;
    }
    Type[] types = delegate.getClass().getGenericInterfaces();
    for (Type t : types) {
      if (t instanceof ParameterizedType) {
        ParameterizedType tt = (ParameterizedType) t;
        if (tt.getRawType() == Consumer.class) {
          type = tt.getActualTypeArguments()[0];
          break;
        }
      }
    }
    return type;
  }
  
  public static void register(String queue, String exchange, String routing, String messageConverter,
                              Consumer<?> delegate) {
    QueueProperties queueProperties = new QueueProperties(queue, exchange, routing, messageConverter);
    CONSUMERS.put(key(queue, exchange, routing), new ConsumerDef(queueProperties, delegate));
  }
  
  public static ConsumerDef lookup(String queue, String exchange, String routing) {
    return CONSUMERS.get(key(queue, exchange, routing));
  }
  
  private static String key(String queue, String exchange, String routing) {
    return String.join("|", queue, exchange, routing);
  }
}
