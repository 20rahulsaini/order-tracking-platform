package com.example.trackingservice;

import com.example.trackingservice.kafka.OrderEventConsumer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

/**
 * Smoke test: verifies the Tracking Service Spring application context loads
 * with H2 instead of MySQL. The Kafka consumer bean is mocked so no live
 * broker is required for the context to start.
 *
 * Note: Spring Boot auto-configures a StringRedisTemplate lazily - it doesn't
 * try to connect on startup, so a missing Redis is fine for context loads.
 */
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.datasource.url=jdbc:h2:mem:trackingdb;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.kafka.bootstrap-servers=localhost:0",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.group-id=tracking-service-group-test"
})
class TrackingServiceApplicationTests {

    @MockBean
    OrderEventConsumer consumer;

    @Test
    void contextLoads() {
        // If the application context fails to load, this test fails.
    }
}
