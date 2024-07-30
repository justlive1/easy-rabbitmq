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

import java.util.Map;
import lombok.Data;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * rabbit 配置
 *
 * @author wubo
 */
@Data
@ConfigurationProperties(EasyRabbitProperties.PREFIX)
public class EasyRabbitProperties {

  public static final String PREFIX = "easy-boot.rabbit";
  public static final String PRIMARY = "primary";


  private String[] basePackages;
  private Map<String, RabbitProperties> sources;
  private ProducerProperties producer;
  private ConsumerProperties consumer;

  @Data
  public static class ProducerProperties {

    private boolean enabled = false;
  }

  @Data
  public static class ConsumerProperties {

    private boolean enabled = false;
    private boolean ackMultiple = false;
    private boolean nackMultiple = false;
    private boolean nackRequeue = true;
  }
}
