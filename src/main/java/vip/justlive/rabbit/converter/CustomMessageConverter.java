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

package vip.justlive.rabbit.converter;

import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import vip.justlive.rabbit.consumer.ConsumerDef;
import vip.justlive.rabbit.producer.QueueProperties;

/**
 * 自定义转换器，处理类名不相同json转换报错问题
 *
 * @author wubo
 */
public class CustomMessageConverter extends AbstractMessageConverter implements
    ApplicationContextAware {

  private ApplicationContext applicationContext;

  @Override
  public Object fromMessage(Message message) {

    MessageProperties prop = message.getMessageProperties();
    ConsumerDef consumer = ConsumerDef.lookup(prop.getConsumerQueue(), prop.getReceivedExchange(),
        prop.getReceivedRoutingKey());
    if (consumer == null) {
      return message.getBody();
    }

    if (applicationContext != null && StringUtils.hasText(
        consumer.getQueueProperties().getMessageConverter())) {
      return applicationContext.getBean(consumer.getQueueProperties().getMessageConverter(),
              MessageConverter.class)
          .fromMessage(message);
    }

    Object msg = message.getBody();
    Type type = consumer.getType();

    String contentType = prop.getContentType();
    if (contentType != null && contentType.startsWith("text")) {
      String encoding = prop.getContentEncoding();
      if (encoding == null) {
        encoding = StandardCharsets.UTF_8.name();
      }
      if (type == String.class) {
        try {
          return new String((byte[]) msg, encoding);
        } catch (UnsupportedEncodingException e) {
          throw new MessageConversionException("failed to convert text-based Message content", e);
        }
      }
    }

    if (type != byte[].class) {
      msg = JSON.parseObject((byte[]) msg, type);
    }
    return msg;
  }

  @Override
  protected Message createMessage(Object object, MessageProperties props) {

    QueueProperties queueProperties = QueueProperties.get();
    if (applicationContext != null && queueProperties != null &&
        StringUtils.hasText(queueProperties.getMessageConverter())) {
      return applicationContext.getBean(queueProperties.getMessageConverter(),
              MessageConverter.class)
          .toMessage(object, props);
    }

    byte[] bytes;
    if (object instanceof byte[]) {
      bytes = (byte[]) object;
      props.setContentType(MessageProperties.CONTENT_TYPE_BYTES);
    } else if (object instanceof String) {
      bytes = ((String) object).getBytes(StandardCharsets.UTF_8);
      props.setContentType(MessageProperties.CONTENT_TYPE_TEXT_PLAIN);
    } else {
      bytes = JSON.toJSONBytes(object);
      props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    }
    props.setContentLength(bytes.length);
    return new Message(bytes, props);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

}

