package com.mdia.platform.shippingservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {
		"app.kafka.ordersTopic=orders.events",
		"app.kafka.shipmentsTopic=shipments.events"
})
@Import(TestcontainersConfiguration.class)
class ShippingServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
