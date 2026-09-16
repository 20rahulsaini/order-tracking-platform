package com.example.orderservice;

import com.example.orderservice.kafka.OrderEventProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Smoke test: verifies the Spring application context loads.
 *
 * Uses an in-memory H2 instead of MySQL. The Kafka producer is mocked so
 * no live broker is required for the context to start.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:orderdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.kafka.bootstrap-servers=localhost:0"
})
class OrderServiceApplicationTests {

    @MockBean
    OrderEventProducer producer;

    @Test
    void contextLoads() {
        // If the application context fails to load, this test fails.
    }
}
