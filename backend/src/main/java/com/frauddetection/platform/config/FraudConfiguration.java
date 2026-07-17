package com.frauddetection.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.frauddetection.platform.service.FraudAssessmentCompletedEvent;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcOperations;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.ott.JdbcOneTimeTokenService;
import org.springframework.security.authentication.ott.OneTimeTokenService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties({
    FraudScoringProperties.class,
    FraudEventProperties.class,
    FraudOutboxProperties.class,
    FraudStepUpProperties.class
})
public class FraudConfiguration {

    @Bean
    ObjectMapper objectMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }

    @Bean
    ProducerFactory<String, FraudAssessmentCompletedEvent> fraudAssessmentProducerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        JacksonJsonSerializer<FraudAssessmentCompletedEvent> valueSerializer =
            new JacksonJsonSerializer<FraudAssessmentCompletedEvent>().noTypeInfo();
        return new DefaultKafkaProducerFactory<>(properties, new StringSerializer(), valueSerializer);
    }

    @Bean
    KafkaTemplate<String, FraudAssessmentCompletedEvent> fraudAssessmentKafkaTemplate(
        ProducerFactory<String, FraudAssessmentCompletedEvent> fraudAssessmentProducerFactory
    ) {
        return new KafkaTemplate<>(fraudAssessmentProducerFactory);
    }

    @Bean
    ProducerFactory<String, Object> fraudEventProducerFactory(
        @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        JacksonJsonSerializer<Object> valueSerializer =
            new JacksonJsonSerializer<Object>().noTypeInfo();
        return new DefaultKafkaProducerFactory<>(properties, new StringSerializer(), valueSerializer);
    }

    @Bean
    KafkaTemplate<String, Object> fraudEventKafkaTemplate(
        ProducerFactory<String, Object> fraudEventProducerFactory
    ) {
        return new KafkaTemplate<>(fraudEventProducerFactory);
    }

    @Bean
    OneTimeTokenService oneTimeTokenService(JdbcOperations jdbcOperations) {
        return new JdbcOneTimeTokenService(jdbcOperations);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
