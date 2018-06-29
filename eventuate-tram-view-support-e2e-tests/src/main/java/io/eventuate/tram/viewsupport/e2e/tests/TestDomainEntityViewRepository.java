package io.eventuate.tram.viewsupport.e2e.tests;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TestDomainEntityViewRepository extends MongoRepository<TestDomainEntityView, Long>, TestDomainEntityViewRepositoryCustom {
}
