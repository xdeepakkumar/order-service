package com.gigaforce.orderservice.external.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gigaforce.orderservice.exception.CustomException;
import com.gigaforce.orderservice.external.response.ErrorResponse;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode (String methodKey, Response response) {
        ObjectMapper objectMapper = new ObjectMapper();
        log.info("::{}", response.request().url());
        log.info("::{}", response.request().headers());
        try {
            ErrorResponse errorResponse = objectMapper.readValue(response.body().asInputStream(), ErrorResponse.class);
            return new CustomException(errorResponse.getErrorMessage(), errorResponse.getErrorCode(),
                                       response.status());
        } catch (IOException e) {
            throw new CustomException("Internal server error", "INTERNAL_SERVER_ERROR", 500);
        }
    }
}
