package com.example.shared.monitoring;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "monitoring.type", havingValue = "prometheus", matchIfMissing = true)
public class MicrometerMetricsAdapter implements MetricsProvider {

    private final MeterRegistry meterRegistry;

    @Override
    public void recordMetric(String name, double value, String... tags) {
        // todo add Atomic for mutable object
        meterRegistry.gauge(name, value);
    }

    @Override
    public void incrementCounter(String name, String... tags) {
        meterRegistry.counter(name, tags).increment();
    }

    @Override
    public void recordExecutionTime(String name, long durationMs, String... tags) {
        Timer.builder(name)
                .tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMs));
    }
}
