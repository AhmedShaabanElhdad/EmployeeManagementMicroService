package com.example.stock

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class StockApplication

fun main(args: Array<String>) {
    runApplication<StockApplication>(*args)
}
