package com.wd.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the YAML ObjectMapper used by HolidaySeeder (PR1) and
 * WbsTemplateSeeder (PR2). Distinct named bean to avoid clobbering the
 * primary Jackson ObjectMapper Spring Boot auto-configures for HTTP.
 */
@Configuration
public class ScheduleSeedingConfig {

    @Bean(name = "scheduleYamlMapper")
    public ObjectMapper scheduleYamlMapper() {
        return new ObjectMapper(new YAMLFactory());
    }
}
