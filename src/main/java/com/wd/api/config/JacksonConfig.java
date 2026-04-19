package com.wd.api.config;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    private static final int MAX_NESTING_DEPTH = 5000;

    @Bean
    public Hibernate6Module hibernate6Module() {
        // Force lazy-loaded associations to serialise as null rather than
        // traversing the Hibernate bytecode-enhanced proxy (which generates
        // infinite nesting for bidirectional relationships).
        Hibernate6Module module = new Hibernate6Module();
        module.configure(Hibernate6Module.Feature.FORCE_LAZY_LOADING, false);
        module.configure(Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS, true);
        return module;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> builder.postConfigurer(this::relaxNestingLimits);
    }

    private void relaxNestingLimits(ObjectMapper mapper) {
        mapper.getFactory().setStreamWriteConstraints(
                StreamWriteConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build());
        mapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder().maxNestingDepth(MAX_NESTING_DEPTH).build());
    }
}
