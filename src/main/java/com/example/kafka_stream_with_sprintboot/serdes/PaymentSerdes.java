package com.example.kafka_stream_with_sprintboot.serdes;

import com.example.kafka_stream_with_sprintboot.events.PaymentEvent;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

/**
 * Requires the WrapperSerdes to allow this to be added as the default serdes config in the KafkaStreams configuration.
 */
public final class PaymentSerdes extends Serdes.WrapperSerde<PaymentEvent> {

    public PaymentSerdes() {
        super(new JsonSerializer<>(), new JsonDeserializer<>(PaymentEvent.class));
    }

    public static Serde<PaymentEvent> serdes() {
        JsonSerializer<PaymentEvent> serializer = new JsonSerializer<>();
        JsonDeserializer<PaymentEvent> deserializer = new JsonDeserializer<>(PaymentEvent.class);
        return Serdes.serdeFrom(serializer, deserializer);
    }
}