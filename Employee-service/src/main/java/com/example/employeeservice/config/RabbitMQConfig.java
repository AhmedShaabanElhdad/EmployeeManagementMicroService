package com.example.employeeservice.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// todo add DLQ
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE = "employee.created.queue";
    public static final String EXCHANGE = "employee.exchange";
    public static final String ROUTING_KEY = "employee.created";

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    Queue queue() {
        return QueueBuilder
                .durable(QUEUE)
                .build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(
            ConnectionFactory connectionFactory
    ) {

        RabbitTemplate template =
                new RabbitTemplate(connectionFactory);

        template.setMessageConverter(
                jsonMessageConverter()
        );

        return template;
    }

    @Bean
    Binding binding(
            Queue queue,
            TopicExchange exchange
    ) {

        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ROUTING_KEY);
    }

}
