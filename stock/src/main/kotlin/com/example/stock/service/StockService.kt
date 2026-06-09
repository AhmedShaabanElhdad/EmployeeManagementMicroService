package com.example.stock.service

import com.example.stock.model.StockPrice
import com.example.stock.repository.StockPriceRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDateTime
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.round

@Service
class StockService(
    private val webClientBuilder: WebClient.Builder,
    private val stockPriceRepository: StockPriceRepository
) {

    private val log = LoggerFactory.getLogger(StockService::class.java)

    @Value("\${external.stock.api.url:https://api.example.com/stocks}")
    private lateinit var externalApiUrl: String

    private val stocks = mutableMapOf(
        "AAPL" to 150.0,
        "GOOGL" to 2800.0,
        "MSFT" to 300.0,
        "AMZN" to 3300.0,
        "TSLA" to 700.0
    )

    /**
     * Fetches the latest price from an external server and persists it to Cassandra.
     * Implements "Error Drop" via Resilience4j Fallback.
     */
    @CircuitBreaker(name = "externalStockApi", fallbackMethod = "fallbackPrice")
    @Retry(name = "externalStockApi")
    fun getLatestPrice(symbol: String): StockPrice {
        log.debug("Fetching price for {} from external API", symbol)

        // Simulating external API call logic
        if (ThreadLocalRandom.current().nextDouble() < 0.2) {
            log.error("External API simulated error for {}", symbol)
            throw RuntimeException("External Server Unavailable")
        }

        val currentPrice = stocks[symbol] ?: 100.0
        val change = ThreadLocalRandom.current().nextDouble(-1.0, 1.0)
        val newPrice = round((currentPrice + change) * 100) / 100.0
        stocks[symbol] = newPrice
        
        val stockPrice = StockPrice(symbol = symbol, price = newPrice, timestamp = LocalDateTime.now())
        
        // Persist to Cassandra
        try {
            stockPriceRepository.save(stockPrice)
            log.trace("Saved stock price to Cassandra for {}", symbol)
        } catch (e: Exception) {
            log.error("Failed to save stock price to Cassandra: {}", e.message)
            // We don't throw here to ensure the real-time stream continues even if DB is down
        }

        return stockPrice
    }

    /**
     * Fallback method handles "Error Drop" by providing the last known price.
     */
    fun fallbackPrice(symbol: String, t: Throwable): StockPrice {
        log.warn("Error fetching price for {}. Dropping error and using fallback. Reason: {}", symbol, t.message)
        val lastPrice = stocks[symbol] ?: 100.0
        return StockPrice(symbol = symbol, price = lastPrice, timestamp = LocalDateTime.now())
    }

    fun getAllSymbols(): Set<String> = stocks.keys
    
    fun getPriceHistory(symbol: String, since: LocalDateTime): List<StockPrice> {
        return stockPriceRepository.findBySymbolAndTimestampAfter(symbol, since)
    }
}
