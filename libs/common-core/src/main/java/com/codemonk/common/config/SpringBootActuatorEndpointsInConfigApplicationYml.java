package com.codemonk.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.List;

@Configuration
public class SpringBootActuatorEndpointsInConfigApplicationYml {

    private static final Logger log = LoggerFactory.getLogger(SpringBootActuatorEndpointsInConfigApplicationYml.class);

    @Value("${management.endpoints.web.exposure.include}")
    private List<String> exposedEndpoints;

    @Value("${management.endpoint.health.show-details}")
    private String healthShowDetails;

    @EventListener(ApplicationReadyEvent.class)
    public void logActuatorConfig() {
        log.info("Actuator endpoints exposed: {}", exposedEndpoints);
        log.info("Actuator health show-details: {}", healthShowDetails);
    }
}