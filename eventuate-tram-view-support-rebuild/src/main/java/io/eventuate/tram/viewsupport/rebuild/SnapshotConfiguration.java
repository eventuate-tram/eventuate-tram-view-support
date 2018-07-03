package io.eventuate.tram.viewsupport.rebuild;

import io.eventuate.javaclient.spring.jdbc.IdGenerator;
import io.eventuate.javaclient.spring.jdbc.IdGeneratorImpl;
import io.eventuate.local.java.kafka.consumer.EventuateKafkaConsumerConfigurationProperties;
import io.eventuate.local.java.kafka.producer.EventuateKafkaProducer;
import io.eventuate.local.java.kafka.producer.EventuateKafkaProducerConfigurationProperties;
import io.eventuate.tram.events.publisher.TramEventsPublisherConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Import(TramEventsPublisherConfiguration.class)
@EnableConfigurationProperties({EventuateKafkaProducerConfigurationProperties.class,
        EventuateKafkaConsumerConfigurationProperties.class})
public class SnapshotConfiguration {

  @Bean
  public DBDialectDeterminer dbDialectDeterminer() {
    return new DBDialectDeterminer();
  }

  @Bean
  public IdGenerator idGenerator() {
    return new IdGeneratorImpl();
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public EventuateKafkaProducer eventuateKafkaProducer(@Value("${eventuatelocal.kafka.bootstrap.servers}") String eventuateKafkaBootstrapServers,
                                                       EventuateKafkaProducerConfigurationProperties eventuateKafkaProducerConfigurationProperties) {
    return new EventuateKafkaProducer(eventuateKafkaBootstrapServers, eventuateKafkaProducerConfigurationProperties);
  }

  @Bean
  public DBLockService dbLockService(JdbcTemplate jdbcTemplate, DBDialectDeterminer dbDialectDeterminer) {
    return new DBLockService(jdbcTemplate, dbDialectDeterminer);
  }
}
