package com.aromastore.orderservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    public static final String PEDIDOS_QUEUE = "pedidos-queue";
    
    @Bean
    public Queue pedidosQueue() {
        return new Queue(PEDIDOS_QUEUE, true);
    }
}
