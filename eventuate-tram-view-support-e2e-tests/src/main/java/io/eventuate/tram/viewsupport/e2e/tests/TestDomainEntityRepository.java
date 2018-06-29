package io.eventuate.tram.viewsupport.e2e.tests;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface TestDomainEntityRepository extends PagingAndSortingRepository<TestDomainEntity, Long> {
}
