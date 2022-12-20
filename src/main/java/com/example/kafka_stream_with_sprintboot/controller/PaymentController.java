package com.example.kafka_stream_with_sprintboot.controller;


import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/kafkastream")
public class PaymentController {

    @Autowired
    private StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @GetMapping("/payment/{balance}")
    public ResponseEntity<Long> getPaymentBalance(@PathVariable String balance) {

        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();
        ReadOnlyKeyValueStore<String, Long> balances = kafkaStreams.store(StoreQueryParameters.fromNameAndType("balance", QueryableStoreTypes.keyValueStore()));
        ResponseEntity response;

        if(balances.get(balance) == null) {
            response = ResponseEntity.notFound().build();
        } else {
            response = ResponseEntity.ok(balances.get(balance));
        }
        return response;
    }
}
