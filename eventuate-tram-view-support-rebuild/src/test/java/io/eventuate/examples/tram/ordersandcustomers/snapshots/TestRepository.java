package io.eventuate.examples.tram.ordersandcustomers.snapshots;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface TestRepository extends PagingAndSortingRepository<TestEntity, Long> {
}
