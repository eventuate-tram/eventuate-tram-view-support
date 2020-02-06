package io.eventuate.tram.viewsupport.rebuild;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.function.Function;

public interface DomainSnapshotExportServiceFactory<T> {
  DomainSnapshotExportService<T> make(Class<T> domainClass,
                                      PagingAndSortingRepository<T, Long> domainRepository,
                                      Function<T, DomainEventWithEntityId> domainEntityToDomainEventConverter,
                                      DBLockService.TableSpec domainTableSpec,
                                      String readerName);
}
