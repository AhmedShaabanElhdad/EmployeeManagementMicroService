package com.example.stock.model

import org.springframework.data.cassandra.core.cql.Ordering
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.Column
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("stock_prices")
data class StockPrice(
    @PrimaryKeyColumn(name = "symbol", type = PrimaryKeyType.PARTITIONED)
    val symbol: String,

    @PrimaryKeyColumn(name = "timestamp", type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column("price")
    val price: Double,

    @Column("id")
    val id: UUID = UUID.randomUUID()
)
