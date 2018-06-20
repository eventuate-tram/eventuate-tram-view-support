package io.eventuate.examples.tram.ordersandcustomers.snapshots;

import io.eventuate.javaclient.spring.jdbc.IdGenerator;
import io.eventuate.javaclient.spring.jdbc.IdGeneratorImpl;
import io.eventuate.local.java.kafka.producer.EventuateKafkaProducer;
import io.eventuate.tram.events.publisher.TramEventsPublisherConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Import(TramEventsPublisherConfiguration.class)
public class SnapshotConfiguration {

  @Bean
  public IdGenerator idGenerator() {
    return new IdGeneratorImpl();
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }

  @Bean
  public EventuateKafkaProducer eventuateKafkaProducer(@Value("${eventuatelocal.kafka.bootstrap.servers}") String eventuateKafkaBootstrapServers) {
    return new EventuateKafkaProducer(eventuateKafkaBootstrapServers);
  }

  @Bean
  public DBLockService dbLockService() {
    return new DBLockService();
  }
}
