package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Configuration;

// 这段代码用于配置 RabbitMQ 的话题模式，包括队列、交换机和绑定。下面是加上注释后的代码：
@Configuration
public class RabbitMQTopicConfig {
    public static final String QUEUE = "seckillQueue"; // 队列名
    public static final String EXCHANGE = "seckillExchange"; // 交换机名
    public static final String ROUTINGKEY = "seckill.lua.#";

    @Bean
    public Queue queue(){
        return new Queue(QUEUE);
    }

    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(){
        return BindingBuilder.bind(queue()).to(topicExchange()).with(ROUTINGKEY);
    }
}


