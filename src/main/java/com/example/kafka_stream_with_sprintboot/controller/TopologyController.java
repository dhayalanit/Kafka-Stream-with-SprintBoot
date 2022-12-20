package com.example.kafka_stream_with_sprintboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/kafkastream")
public class TopologyController {

    @Autowired
    private StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    @GetMapping("/getTopology")
    public ResponseEntity<String> getTopology() {
        return ResponseEntity.ok(streamsBuilderFactoryBean.getTopology().describe().toString());
    }
}
