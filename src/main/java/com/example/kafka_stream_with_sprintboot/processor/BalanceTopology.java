package com.example.kafka_stream_with_sprintboot.processor;


import com.example.kafka_stream_with_sprintboot.events.PaymentEvent;
import com.example.kafka_stream_with_sprintboot.properties.KafkaStreamsProperties;
import com.example.kafka_stream_with_sprintboot.serdes.PaymentSerdes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BalanceTopology {

    @Autowired
    private KafkaStreamsProperties kafkaStreamsProperties;

    private static List SUPPORTED_RAILS = Arrays.asList(Rails.BANK_RAILS_BAR.name(), Rails.BANK_RAILS_FOO.name());

    private static final Serde<String> STRING_SERDE = Serdes.String();
    private static final Serde<Long> LONG_SERDE = Serdes.Long();


    @Autowired
    public void buildPipeLine(StreamsBuilder streamsBuilder) {

        KStream<String, PaymentEvent> paymentEventKStream = streamsBuilder
                .stream(kafkaStreamsProperties.getPaymentInboundTopic(), Consumed.with(STRING_SERDE, PaymentSerdes.serdes()))
                .peek((key,payment) -> log.info("Payment event received with key=" + key + ", payment=" + payment))
                // Filter out unsupported bank rails.
                .filter((key, payment) -> SUPPORTED_RAILS.contains(payment.getRails()))
                .peek((key, value) -> log.info("Filtered payment event received with key=" + key + ", value=" + value));

        // Branch based on currency in order to perform any FX.
        KStream<String, PaymentEvent>[] currenciesBranches = paymentEventKStream.branch(
                (key, payment) -> payment.getCurrency().equals(Currency.GBP.name()),
                (key, payment) -> payment.getCurrency().equals(Currency.USD.name())
        );

        KStream<String, PaymentEvent> fxStream = currenciesBranches[1].mapValues(
                // Use mapValues() as we are transforming the payment, but not changing the key.
                (payment) -> {
                    double usdtogbpRate = 0.8;
                    PaymentEvent transformedPayment = PaymentEvent.builder()
                            .paymentId(payment.getPaymentId())
                            .amount(payment.getAmount())
                            .currency(Currency.GBP.name())
                            .fromAccount(payment.getFromAccount())
                            .toAccount(payment.getToAccount())
                            .rails(payment.getRails())
                            .build();

                    return transformedPayment;
                }
        );

        // Merge the payment streams back together.

        KStream<String, PaymentEvent> mergedStreams = currenciesBranches[0].merge(fxStream)
                .peek((key, value) -> log.info("Merged payment event received with key=\" + key + \", value=\" + value)"));

        // Create the KTable stateful store to track account balances.
        mergedStreams
                .map((key, payment) -> new KeyValue<>(payment.getFromAccount(), payment.getAmount()))
                .groupByKey(Grouped.with(STRING_SERDE, LONG_SERDE))
                .aggregate(new Initializer<Long>() {
                               @Override
                               public Long apply() {
                                   return 0L;
                               }
                           }, new Aggregator<String, Long, Long>() {
                               @Override
                               public Long apply(String key, Long Value, Long aggregate) {
                                   return aggregate+Value;
                               }
                           }, Materialized.with(STRING_SERDE, LONG_SERDE).as("balance")

                );

        // Branch based on bank rails for outbound publish.

        KStream<String, PaymentEvent>[] railsBranches = mergedStreams.branch(
                (key, payment) -> payment.getRails().equals(Rails.BANK_RAILS_FOO.name()),
                (key, payment) -> payment.getRails().equals(Rails.BANK_RAILS_BAR.name()));

        // Publish outbound events.
        railsBranches[0].to(kafkaStreamsProperties.getRailsBarOutboundTopic(), Produced.with(STRING_SERDE, PaymentSerdes.serdes()));
        railsBranches[1].to(kafkaStreamsProperties.getRailsFooOutboundTopic(), Produced.with(STRING_SERDE, PaymentSerdes.serdes()));
    }
}
