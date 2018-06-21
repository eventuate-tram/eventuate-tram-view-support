package io.eventuate.tram.viewsupport.rebuild;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface TestRepository extends PagingAndSortingRepository<TestEntity, Long> {
}
