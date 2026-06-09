package com.example.stock.config

import org.springframework.amqp.core.*
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitMQConfig {

    companion object {
        const val STOCK_PRICE_QUEUE = "stock.price.queue"
        const val STOCK_PRICE_EXCHANGE = "stock.price.exchange"
        const val STOCK_PRICE_ROUTING_KEY = "stock.price.routingKey"
    }

    @Bean
    fun queue(): Queue = Queue(STOCK_PRICE_QUEUE)

    @Bean
    fun exchange(): TopicExchange = TopicExchange(STOCK_PRICE_EXCHANGE)

    @Bean
    fun binding(queue: Queue, exchange: TopicExchange): Binding {
        return BindingBuilder.bind(queue).to(exchange).with(STOCK_PRICE_ROUTING_KEY)
    }

    @Bean
    fun converter(): MessageConverter = Jackson2JsonMessageConverter()

    @Bean
    fun amqpTemplate(connectionFactory: ConnectionFactory): AmqpTemplate {
        val rabbitTemplate = RabbitTemplate(connectionFactory)
        rabbitTemplate.messageConverter = converter()
        return rabbitTemplate
    }
}
