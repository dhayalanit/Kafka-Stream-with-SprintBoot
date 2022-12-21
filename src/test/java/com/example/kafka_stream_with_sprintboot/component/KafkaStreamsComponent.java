package com.example.kafka_stream_with_sprintboot.component;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.KafkaClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;
import java.util.UUID;

@Slf4j
public class KafkaStreamsComponent {

    private final static String GROUP_ID ="KafkaStreamsComponentTest";
    private final static String PAYMENT_TOPIC = "payment-topic";
    private final static String RAILS_FOO_TOPIC = "rails-foo-topic";
    private final static String RAILS_BAR_TOPIC = "rails-bar-topic";

    // Accounts
    private static final String ACCOUNT_XXX = "XXX_"+ UUID.randomUUID();
    private static final String ACCOUNT_YYY = "YYY_"+ UUID.randomUUID();
    private static final String ACCOUNT_ZZZ = "ZZZ_"+ UUID.randomUUID();

    // One destination account for all tests.
    private static final String ACCOUNT_DEST = UUID.randomUUID().toString();

    private Consumer fooRailsConsumer;
    private Consumer barRailsConsumer;

    private final static Random RANDOM = new Random();

    @BeforeEach
    public void setUp() {

        ///https://github.com/lydtechconsulting/kafka-streams/blob/main/src/test/java/demo/kafka/streams/component/KafkaStreamsCT.java

    }

}
