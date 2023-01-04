package com.sbootprojects.cryptoranking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// it is a configuration
@Configuration
public class CustomWebConfig {

    // restTemplate to call the api
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
