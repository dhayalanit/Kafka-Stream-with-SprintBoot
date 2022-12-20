package com.example.kafka_stream_with_sprintboot.exception;

public class KafkaStreamsException extends RuntimeException {

    public KafkaStreamsException (Throwable cause) {
        super(cause);
    }
}
