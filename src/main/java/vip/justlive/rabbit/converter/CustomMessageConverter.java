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

package vip.justlive.rabbit.converter;

import com.alibaba.fastjson.JSON;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;

/**
 * 自定义转换器，处理类名不相同json转换报错问题
 *
 * @author wubo
 */
public class CustomMessageConverter extends AbstractMessageConverter {
  
  public static final CustomMessageConverter INS = new CustomMessageConverter();
  
  @Override
  public Object fromMessage(Message message) {
    return message.getBody();
  }
  
  @Override
  protected Message createMessage(Object object, MessageProperties props) {
    byte[] bytes;
    if (object instanceof byte[]) {
      bytes = (byte[]) object;
      props.setContentType(MessageProperties.CONTENT_TYPE_BYTES);
    } else {
      bytes = JSON.toJSONBytes(object);
      props.setContentType(MessageProperties.CONTENT_TYPE_SERIALIZED_OBJECT);
    }
    props.setContentLength(bytes.length);
    return new Message(bytes, props);
  }
}

