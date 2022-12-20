package com.example.kafka_stream_with_sprintboot.properties;


import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties("kafkastreamsdemo")
@Setter
@Getter
@Validated
public class KafkaStreamsProperties {
    @NonNull private String id;
    @NonNull private String paymentInboundTopic;
    @NonNull private String railsFooOutboundTopic;
    @NonNull private String railsBarOutboundTopic;
}
