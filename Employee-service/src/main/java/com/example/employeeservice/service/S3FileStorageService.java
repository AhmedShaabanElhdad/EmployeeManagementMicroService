package com.example.employeeservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.ByteBuffer;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3FileStorageService {

    private final S3AsyncClient s3AsyncClient;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.endpoint}")
    private String endpoint;

    public Mono<String> uploadFile(FilePart file) {
        String fileName = UUID.randomUUID() + "_" + file.filename();
        
        return file.content()
                .map(dataBuffer -> ByteBuffer.wrap(dataBuffer.asByteBuffer().array()))
                .collectList()
                .flatMap(list -> {
                    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(fileName)
                            .contentType(file.headers().getContentType().toString())
                            .build();

                    // For simplicity, collecting to a single byte array. 
                    // For very large files, a more streaming approach would be better.
                    int totalSize = list.stream().mapToInt(ByteBuffer::remaining).sum();
                    byte[] allBytes = new byte[totalSize];
                    int offset = 0;
                    for (ByteBuffer bb : list) {
                        int len = bb.remaining();
                        bb.get(allBytes, offset, len);
                        offset += len;
                    }

                    return Mono.fromFuture(s3AsyncClient.putObject(putObjectRequest, AsyncRequestBody.fromBytes(allBytes)))
                            .doOnSuccess(response -> log.info("Successfully uploaded file {} to S3", fileName))
                            .thenReturn(endpoint + "/" + bucketName + "/" + fileName);
                });
    }
}
