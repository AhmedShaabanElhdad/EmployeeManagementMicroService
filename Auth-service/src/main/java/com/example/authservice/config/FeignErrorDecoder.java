package com.example.authservice.config;

import com.example.shared.core.CustomResponseException;

import feign.Response;
import feign.codec.ErrorDecoder;

public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(
            String methodKey,
            Response response) {

        switch (response.status()) {

            case 404:
                return CustomResponseException.ResourceNotFound(
                        "Employee not found");

            case 401:
                return CustomResponseException.BadCredential();

            case 400:
                return CustomResponseException.BadRequest(
                        "Bad Request");

            default:
                return CustomResponseException.InternalServerError(
                        "Remote service error");
        }
    }
}