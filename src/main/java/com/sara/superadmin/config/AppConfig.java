package com.sara.superadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableScheduling
public class AppConfig {

	@Bean
	public RestTemplate livenessRestTemplate(RestTemplateBuilder builder,
											 @Value("${super-admin.liveness.timeout-ms}") long timeoutMs) {
		return builder
				.connectTimeout(Duration.ofMillis(timeoutMs))
				.readTimeout(Duration.ofMillis(timeoutMs))
				.build();
	}
}
