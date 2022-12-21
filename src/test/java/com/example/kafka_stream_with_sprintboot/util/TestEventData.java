package com.example.kafka_stream_with_sprintboot.util;

import com.example.kafka_stream_with_sprintboot.events.PaymentEvent;

public class TestEventData {

    public static PaymentEvent buildPaymentEvent(String id, Long amount, String currency, String fromAccount, String toAccount, String rails) {
        return PaymentEvent.builder()
                .paymentId(id)
                .amount(amount)
                .currency(currency)
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .rails(rails)
                .build();
    }
}
