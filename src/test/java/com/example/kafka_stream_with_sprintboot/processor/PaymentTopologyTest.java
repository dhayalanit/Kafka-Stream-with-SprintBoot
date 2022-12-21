package com.example.kafka_stream_with_sprintboot.processor;

import com.example.kafka_stream_with_sprintboot.events.PaymentEvent;
import com.example.kafka_stream_with_sprintboot.properties.KafkaStreamsProperties;
import com.example.kafka_stream_with_sprintboot.serdes.PaymentSerdes;
import com.example.kafka_stream_with_sprintboot.util.TestEventData;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.example.kafka_stream_with_sprintboot.util.TestEventData.buildPaymentEvent;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testcontainers.shaded.org.hamcrest.MatcherAssert.assertThat;
import static org.testcontainers.shaded.org.hamcrest.core.IsEqual.equalTo;
import static org.testcontainers.shaded.org.hamcrest.core.IsIterableContaining.hasItems;
import static org.testcontainers.shaded.org.hamcrest.core.IsNull.nullValue;

import java.util.Properties;
import java.util.UUID;

public class PaymentTopologyTest {


    private KafkaStreamsProperties properties;
    private BalanceTopology paymentTopology;

    private static final String PAYMENT_INBOUND_TOPIC = "payment-topic";
    private static final String RAILS_FOO_OUTBOUND_TOPIC = "rails-foo-topic";
    private static final String RAILS_BAR_OUTBOUND_TOPIC = "rails-BAR-topic";

    // GBP Accounts.
    private static final String ACCOUNT_GBP_ABC = "ABC-"+ UUID.randomUUID();
    private static final String ACCOUNT_GBP_DEF = "DEF-"+UUID.randomUUID();

    // USD Accounts.
    private static final String ACCOUNT_USD_XYZ = "XYZ-"+UUID.randomUUID();



    @BeforeEach
    public void setUp() {
        properties = mock(KafkaStreamsProperties.class);
        when(properties.getPaymentInboundTopic()).thenReturn(PAYMENT_INBOUND_TOPIC);
        when(properties.getRailsFooOutboundTopic()).thenReturn(RAILS_FOO_OUTBOUND_TOPIC);
        when(properties.getRailsBarOutboundTopic()).thenReturn(RAILS_BAR_OUTBOUND_TOPIC);
        paymentTopology = new BalanceTopology(properties);
    }

    @Test
    public void testPaymentTopology() throws Exception {
        StreamsBuilder streamsBuilder = new StreamsBuilder();
        paymentTopology.buildPipeLine(streamsBuilder);

        Topology topology = streamsBuilder.build();

        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(DEFAULT_KEY_SERDE_CLASS_CONFIG,   Serdes.String().getClass().getName());
        streamsConfiguration.put(DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.Long().getClass().getName());

        TopologyTestDriver topologyTestDriver = new TopologyTestDriver(topology, streamsConfiguration);
        TestInputTopic<String, PaymentEvent> inputTopic = topologyTestDriver
                .createInputTopic(PAYMENT_INBOUND_TOPIC, new StringSerializer(), PaymentSerdes.serdes().serializer());

        TestOutputTopic<String, PaymentEvent> railsFooOutputTopic = topologyTestDriver
                .createOutputTopic(RAILS_FOO_OUTBOUND_TOPIC, new StringDeserializer(), PaymentSerdes.serdes().deserializer());
        TestOutputTopic<String, PaymentEvent> railsBarOutputTopic = topologyTestDriver
                .createOutputTopic(RAILS_BAR_OUTBOUND_TOPIC, new StringDeserializer(), PaymentSerdes.serdes().deserializer());

        // Three payments via FOO rails from ABC to DEF, total 210 GBP.
        PaymentEvent payment1 = buildPaymentEvent(UUID.randomUUID().toString(),
                100L,
                "GBP",
                ACCOUNT_GBP_ABC,
                ACCOUNT_GBP_DEF,
                Rails.BANK_RAILS_FOO.name());
        inputTopic.pipeInput(payment1.getPaymentId(), payment1);
        PaymentEvent payment2 = buildPaymentEvent(UUID.randomUUID().toString(),
                50L,
                "GBP",
                ACCOUNT_GBP_ABC,
                ACCOUNT_GBP_DEF,
                Rails.BANK_RAILS_FOO.name());
        inputTopic.pipeInput(payment2.getPaymentId(), payment2);
        PaymentEvent payment3 = buildPaymentEvent(UUID.randomUUID().toString(),
                60L,
                "GBP",
                ACCOUNT_GBP_ABC,
                ACCOUNT_GBP_DEF,
                Rails.BANK_RAILS_FOO.name());
        inputTopic.pipeInput(payment3.getPaymentId(), payment3);

        // Payment on an unsupported rails should be filtered out.
        PaymentEvent payment4 = buildPaymentEvent(UUID.randomUUID().toString(),
                1200L,
                "GBP",
                ACCOUNT_GBP_ABC,
                ACCOUNT_GBP_DEF,
                Rails.BANK_RAILS_XXX.name());
        inputTopic.pipeInput(payment4.getPaymentId(), payment4);

        // Payment from a USD account will require FX.
        PaymentEvent payment5 = buildPaymentEvent(UUID.randomUUID().toString(),
                1000L,  // Converts to 800 GBP.
                "USD",
                ACCOUNT_USD_XYZ,
                ACCOUNT_GBP_DEF,
                Rails.BANK_RAILS_BAR.name());
        inputTopic.pipeInput(payment5.getPaymentId(), payment5);

        // Assert the outbound rails topics have the expected events.
        assertThat(railsFooOutputTopic.readKeyValuesToList(),
                hasItems(
                        KeyValue.pair(payment1.getPaymentId(), payment1),
                        KeyValue.pair(payment2.getPaymentId(), payment2),
                        KeyValue.pair(payment3.getPaymentId(), payment3)
                ));

        // Expected event after FX transform.
        PaymentEvent payment5fx = buildPaymentEvent(payment5.getPaymentId(),
                800L,
                "GBP",  // Converted from 1000 USD.
                payment5.getFromAccount(),
                payment5.getToAccount(),
                payment5.getRails());
        assertThat(railsBarOutputTopic.readKeyValuesToList(),
                hasItems(
                        KeyValue.pair(payment5.getPaymentId(), payment5fx)
                ));

        // Expect the balances are correctly aggregated in the state store.
        KeyValueStore<String, Long> balanceStore = topologyTestDriver.getKeyValueStore("balance");
        assertThat(balanceStore.get(ACCOUNT_GBP_ABC), equalTo(210L)); // Payments: 100 + 60 + 50.
        assertThat(balanceStore.get(ACCOUNT_GBP_DEF), nullValue()); // No payments from this account.
        assertThat(balanceStore.get(ACCOUNT_USD_XYZ), equalTo(800L)); // 1000 USD * 0.8 FX.


    }
}
