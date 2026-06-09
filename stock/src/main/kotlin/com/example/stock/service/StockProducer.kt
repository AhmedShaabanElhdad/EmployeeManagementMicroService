package com.example.stock.service

import com.example.stock.config.RabbitMQConfig
import com.example.stock.model.StockPrice
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.round

@Service
class StockProducer(private val rabbitTemplate: RabbitTemplate) {

    private val log = LoggerFactory.getLogger(StockProducer::class.java)

    private val stocks = mutableMapOf(
        "AAPL" to 150.0,
        "GOOGL" to 2800.0,
        "MSFT" to 300.0,
        "AMZN" to 3300.0,
        "TSLA" to 700.0
    )

    @Scheduled(fixedRate = 3000)
    fun produceStockPrices() {
        stocks.keys.forEach { symbol ->
            val price = simulateExternalFetch(symbol)
            log.info("Producing stock price for {}: {}", symbol, price.price)
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.STOCK_PRICE_EXCHANGE,
                RabbitMQConfig.STOCK_PRICE_ROUTING_KEY,
                price
            )
        }
    }

    private fun simulateExternalFetch(symbol: String): StockPrice {
        val currentPrice = stocks[symbol] ?: 100.0
        val change = ThreadLocalRandom.current().nextDouble(-1.0, 1.0)
        val newPrice = round((currentPrice + change) * 100) / 100.0
        stocks[symbol] = newPrice
        return StockPrice(symbol = symbol, price = newPrice, timestamp = LocalDateTime.now())
    }
}
