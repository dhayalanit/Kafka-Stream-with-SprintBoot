package com.example.kafka_stream_with_sprintboot.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private String paymentId;

    private Long amount;

    private String currency;

    private String toAccount;

    private String fromAccount;

    private String rails;
}
