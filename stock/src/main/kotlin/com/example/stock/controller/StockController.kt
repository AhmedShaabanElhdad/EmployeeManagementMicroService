package com.example.stock.controller

import com.example.stock.model.StockPrice
import com.example.stock.service.StockService
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/v1/stocks")
class StockController(private val stockService: StockService) {

    private val log = LoggerFactory.getLogger(StockController::class.java)

    @GetMapping("/{symbol}/real-time", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamStockPrice(@PathVariable symbol: String): SseEmitter {
        val emitter = SseEmitter(Long.MAX_VALUE)
        val executor = Executors.newSingleThreadScheduledExecutor()

        executor.scheduleAtFixedRate({
            try {
                val price = stockService.getLatestPrice(symbol)
                emitter.send(SseEmitter.event()
                    .name("stock-price")
                    .data(price))
            } catch (e: Exception) {
                if (e is IOException) {
                    log.info("Client disconnected for symbol: {}", symbol)
                    emitter.complete()
                    executor.shutdown()
                } else {
                    // "Error Drop": Log the error but keep the stream alive
                    log.error("Dropping stock update error for {}: {}", symbol, e.message)
                }
            }
        }, 0, 3, TimeUnit.SECONDS)

        emitter.onCompletion { executor.shutdown() }
        emitter.onTimeout { executor.shutdown() }

        return emitter
    }

    @GetMapping("/{symbol}/history")
    fun getStockHistory(
        @PathVariable symbol: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) since: LocalDateTime
    ): List<StockPrice> {
        return stockService.getPriceHistory(symbol, since)
    }

    @GetMapping
    fun getSymbols(): Set<String> = stockService.getAllSymbols()
}
