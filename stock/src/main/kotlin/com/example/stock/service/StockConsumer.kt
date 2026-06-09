package com.example.stock.service

import com.example.stock.config.RabbitMQConfig
import com.example.stock.model.StockPrice
import com.example.stock.repository.StockPriceRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class StockConsumer(
    private val stockPriceRepository: StockPriceRepository,
    private val stockService: StockService
) {
    private val log = LoggerFactory.getLogger(StockConsumer::class.java)

    @RabbitListener(queues = [RabbitMQConfig.STOCK_PRICE_QUEUE])
    fun consumeStockPrice(stockPrice: StockPrice) {
        log.info("Consumed stock price for {}: {}", stockPrice.symbol, stockPrice.price)
        
        // 1. Update the in-memory cache in StockService for real-time SSE
        stockService.updateLocalCache(stockPrice)
        
        // 2. Persist to Cassandra for history
        try {
            stockPriceRepository.save(stockPrice)
        } catch (e: Exception) {
            log.error("Failed to persist consumed stock price: {}", e.message)
        }
    }
}
