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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CustomMessageConverter extends AbstractMessageConverter
    implements ApplicationContextAware {

  private ObjectMapper objectMapper;
  private ApplicationContext applicationContext;

  @Override
  public Object fromMessage(Message message) {

    MessageProperties prop = message.getMessageProperties();
    ConsumerDef consumer =
        ConsumerDef.lookup(
            prop.getConsumerQueue(), prop.getReceivedExchange(), prop.getReceivedRoutingKey());
    if (consumer == null) {
      return message.getBody();
    }

    if (applicationContext != null
        && StringUtils.hasText(consumer.getQueueProperties().messageConverter())) {
      return applicationContext
          .getBean(consumer.getQueueProperties().messageConverter(), MessageConverter.class)
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
      try {
        msg =
            getObjectMapper()
                .readValue((byte[]) msg, getObjectMapper().getTypeFactory().constructType(type));
      } catch (IOException e) {
        throw new MessageConversionException("failed to convert Message content", e);
      }
    }
    return msg;
  }

  @Override
  protected Message createMessage(Object object, MessageProperties props) {

    QueueProperties queueProperties = QueueProperties.get();
    if (applicationContext != null
        && queueProperties != null
        && StringUtils.hasText(queueProperties.messageConverter())) {
      return applicationContext
          .getBean(queueProperties.messageConverter(), MessageConverter.class)
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
      try {
        bytes = getObjectMapper().writeValueAsBytes(object);
      } catch (IOException e) {
        throw new MessageConversionException("failed to convert Message content", e);
      }
      props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    }
    props.setContentLength(bytes.length);
    return new Message(bytes, props);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
  }

  public ObjectMapper getObjectMapper() {
    if (objectMapper != null) {
      return objectMapper;
    }
    synchronized (this) {
      if (objectMapper != null) {
        return objectMapper;
      }
      if (applicationContext != null) {
        try {
          objectMapper = applicationContext.getBean(ObjectMapper.class);
        } catch (Exception e) {
          log.warn("ObjectMapper not found in application context, create ObjectMapper");
          objectMapper = new ObjectMapper();
        }
      } else {
        log.warn("ApplicationContext not available, create ObjectMapper");
        objectMapper = new ObjectMapper();
      }
      return objectMapper;
    }
  }
}
