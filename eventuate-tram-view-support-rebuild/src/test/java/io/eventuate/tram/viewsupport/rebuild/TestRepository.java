package io.eventuate.tram.viewsupport.rebuild;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestRepository extends PagingAndSortingRepository<TestEntity, Long> {
}
