package com.example.employeeservice.config;

import org.slf4j.MDC;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class CorrelationClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String>
            CORRELATION_ID_KEY =
            Metadata.Key.of(
                    "x-correlation-id",
                    Metadata.ASCII_STRING_MARSHALLER
            );

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall
                .SimpleForwardingClientCall<>(
                next.newCall(method, callOptions)
        ) {

            @Override
            public void start(
                    Listener<RespT> responseListener,
                    Metadata headers) {

                String correlationId =
                        MDC.get("correlationId");

                if (correlationId != null) {
                    headers.put(
                            CORRELATION_ID_KEY,
                            correlationId
                    );
                }

                super.start(responseListener, headers);
            }
        };
    }
}
