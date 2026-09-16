package com.example.deliveryservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=localhost:0",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class DeliveryServiceApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test
    }
}
