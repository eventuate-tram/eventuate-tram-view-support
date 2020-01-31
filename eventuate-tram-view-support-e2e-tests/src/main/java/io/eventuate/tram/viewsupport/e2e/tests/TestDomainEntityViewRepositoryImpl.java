package io.eventuate.tram.viewsupport.e2e.tests;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class TestDomainEntityViewRepositoryImpl implements TestDomainEntityViewRepositoryCustom {

  private MongoTemplate mongoTemplate;

  @Autowired
  public TestDomainEntityViewRepositoryImpl(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public void addTestDomainEntityView(Long id, String data) {
    mongoTemplate.upsert(new Query(where("id").is(id)),
            new Update().set("data", data), TestDomainEntityView.class);
  }
}
