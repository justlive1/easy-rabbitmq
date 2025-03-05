module easy.rabbitmq {

  requires com.rabbitmq.client;
  requires jakarta.annotation;
  requires spring.amqp;
  requires spring.beans;
  requires spring.context;
  requires spring.core;
  requires spring.boot;
  requires spring.boot.autoconfigure;
  requires spring.rabbit;
  requires org.slf4j;

  requires static lombok;
  requires static fastjson;

  exports vip.justlive.rabbit;
  exports vip.justlive.rabbit.annotation;
  exports vip.justlive.rabbit.consumer;
  exports vip.justlive.rabbit.converter;
  exports vip.justlive.rabbit.producer;
}