# easy-rabbitmq

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/vip.justlive/easy-rabbitmq/badge.svg)](https://maven-badges.herokuapp.com/maven-central/vip.justlive/easy-rabbitmq/)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)


提供基于注解和接口自动装载的封装

```
// 生产端
// 定义接口继承BaseProducer
// 加上@Rqueue注解

// 简单队列模式
@Rqueue(queue = "q")
public interface Q1 extends BaseProducer<String> {

}

// 使用交换器
@Rqueue(queue = "q", exchange = "e")
public interface Q2 extends BaseProducer<CustomObject> {

}

// 增加路由key
@Rqueue(queue = "q", exchange = "e", routing = "r")
public interface Q3 extends BaseProducer<byte[]> {

}

// 设置分发模式
@Rqueue(queue = "q", exchange = "e", routing = "r", exchangeType = "direct")
public interface Q4 extends BaseProducer<String> {

}

// 配置扫描接口路径
@RqueueScan("xxx.xxx")
@Configuration
public class RabbitConfiguration {
}

@Component
public class Demo {

  @Autowired
  Q1 q1;
  @Autowired
  Q2 q2;
  @Autowired
  Q3 q3;
  @Autowired
  Q4 q4;

  @PostConstruct
  private void init(){
  
    q1.send("hello world");
    q2.send(new CustomObject());
    q3.send("hello world".getBytes());
    q4.send("hi");
  }
}


// 消费端
// 修改配置文件， 默认为false不开启
spring.rabbitmq.listener.enabled=true

// 实现Customer接口并增加@Rqueue注解

@Rqueue(queue = "q", exchange = "3")
public class StrMessageProcess implements Consumer<CustomObject> {

  @Override
  public void accept(CustomObject message) {
    System.out.println(message);
  }
}

// 其他配置(参照Springboot官方配置)
spring:
  rabbitmq:
    host: 10.10.30.137
    port: 5672
    username: dev
    password: 123456

```