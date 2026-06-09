package com.example.stock.service

import com.example.stock.model.StockPrice
import com.example.stock.repository.StockPriceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Service
class StockService(
    private val stockPriceRepository: StockPriceRepository
) {

    private val log = LoggerFactory.getLogger(StockService::class.java)

    // Thread-safe local cache for real-time SSE delivery, updated by StockConsumer
    private val stocks = ConcurrentHashMap<String, Double>()

    init {
        // Initial seed data
        stocks["AAPL"] = 150.0
        stocks["GOOGL"] = 2800.0
        stocks["MSFT"] = 300.0
        stocks["AMZN"] = 3300.0
        stocks["TSLA"] = 700.0
    }

    /**
     * Called by StockConsumer when a new price message is received from RabbitMQ.
     */
    fun updateLocalCache(stockPrice: StockPrice) {
        stocks[stockPrice.symbol] = stockPrice.price
        log.trace("Updated local cache for {}: {}", stockPrice.symbol, stockPrice.price)
    }

    /**
     * Returns the latest price from the local cache.
     * This is called by the SseEmitter loop in StockController.
     */
    fun getLatestPrice(symbol: String): StockPrice {
        val currentPrice = stocks[symbol] ?: 100.0
        return StockPrice(
            symbol = symbol,
            price = currentPrice,
            timestamp = LocalDateTime.now()
        )
    }

    fun getAllSymbols(): Set<String> = stocks.keys
    
    fun getPriceHistory(symbol: String, since: LocalDateTime): List<StockPrice> {
        return stockPriceRepository.findBySymbolAndTimestampAfter(symbol, since)
    }
}
