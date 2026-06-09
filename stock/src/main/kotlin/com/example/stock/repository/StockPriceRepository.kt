package com.example.stock.repository

import com.example.stock.model.StockPrice
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface StockPriceRepository : CassandraRepository<StockPrice, String> {
    fun findBySymbolAndTimestampAfter(symbol: String, timestamp: LocalDateTime): List<StockPrice>
}
