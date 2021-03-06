package io.eventuate.tram.viewsupport.rebuild;

import io.eventuate.common.id.IdGenerator;
import io.eventuate.common.jdbc.EventuateCommonJdbcOperations;
import io.eventuate.common.jdbc.EventuateSchema;
import io.eventuate.common.spring.jdbc.EventuateCommonJdbcOperationsConfiguration;
import io.eventuate.messaging.kafka.producer.EventuateKafkaProducer;
import io.eventuate.messaging.kafka.producer.EventuateKafkaProducerConfigurationProperties;
import io.eventuate.messaging.kafka.spring.producer.EventuateKafkaProducerSpringConfigurationProperties;
import io.eventuate.messaging.kafka.spring.producer.EventuateKafkaProducerSpringConfigurationPropertiesConfiguration;
import io.eventuate.tram.spring.events.publisher.TramEventsPublisherConfiguration;
import io.eventuate.tram.spring.messaging.producer.jdbc.TramMessageProducerJdbcConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Import({TramEventsPublisherConfiguration.class,
        TramMessageProducerJdbcConfiguration.class,
        EventuateKafkaProducerSpringConfigurationPropertiesConfiguration.class,
        EventuateCommonJdbcOperationsConfiguration.class})
@EnableConfigurationProperties({EventuateKafkaProducerSpringConfigurationProperties.class})
public class SnapshotConfiguration {

  @Bean
  public DBDialectDeterminer dbDialectDeterminer() {
    return new DBDialectDeterminer();
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

  @Bean
  public SnapshotterConfigurationProperties snapshotterConfigurationProperties() {
    return new SnapshotterConfigurationProperties();
  }

  @Bean
  public <T> DomainSnapshotExportServiceFactory<T> domainSnapshotExportServiceFactory(EventuateSchema eventuateSchema,
                                                                                      EventuateCommonJdbcOperations eventuateCommonJdbcOperations,
                                                                                      EventuateKafkaProducer eventuateKafkaProducer,
                                                                                      DBLockService dbLockService,
                                                                                      IdGenerator idGenerator,
                                                                                      SnapshotterConfigurationProperties snapshotterConfigurationProperties) {
    return (domainClass, domainRepository, domainEntityToDomainEventConverter, domainTableSpec, readerName) ->
      new DomainSnapshotExportService<>(eventuateSchema,
              eventuateCommonJdbcOperations,
              eventuateKafkaProducer,
              dbLockService,
              idGenerator,
              domainClass,
              domainRepository,
              domainEntityToDomainEventConverter,
              domainTableSpec,
              snapshotterConfigurationProperties.getDomainRepositoryPageSize(),
              snapshotterConfigurationProperties.getCdcServiceUrl(),
              snapshotterConfigurationProperties.getCdcStatusServiceEndPoint(),
              readerName,
              snapshotterConfigurationProperties.getMaxIterationsToCheckCdcProcessing(),
              snapshotterConfigurationProperties.getTimeoutBetweenCdcProcessingCheckingIterationsInMilliseconds());
  }
}
