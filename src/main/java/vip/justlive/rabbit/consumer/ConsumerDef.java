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

package vip.justlive.rabbit.consumer;

import lombok.Getter;

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
  
  private final String queue;
  private final String exchange;
  private final String routing;
  private final String messageConverter;
  private final Consumer<Object> delegate;
  
  private Type type;
  
  @SuppressWarnings("unchecked")
  private ConsumerDef(String queue, String exchange, String routing, String messageConverter, Consumer<?> delegate) {
    this.queue = queue;
    this.exchange = exchange;
    this.routing = routing;
    this.messageConverter = messageConverter;
    this.delegate = (Consumer<Object>) delegate;
  }
  
  @Override
  public void accept(Object msg) {
    this.delegate.accept(msg);
  }
  
  Type getType() {
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
  
  private String key() {
    return key(queue, exchange, routing);
  }
  
  private static String key(String queue, String exchange, String routing) {
    return String.join("|", queue, exchange, routing);
  }
  
  
  public static void register(String queue, String exchange, String routing, String messageConverter,
                              Consumer<?> delegate) {
    ConsumerDef definition = new ConsumerDef(queue, exchange, routing, messageConverter, delegate);
    CONSUMERS.put(definition.key(), definition);
  }
  
  public static ConsumerDef lookup(String queue, String exchange, String routing) {
    return CONSUMERS.get(key(queue, exchange, routing));
  }
}
