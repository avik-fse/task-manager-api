package casestudy.taskmanager.repositories;

import casestudy.taskmanager.domains.Task;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Log4j2
@Repository
public class TaskManagerRepoImpl implements TaskManagerRepository {
  private final MongoTemplate mongoTemplate;

  public TaskManagerRepoImpl(final MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public List<Task> findByAllTaskFields(
      final Long parentId,
      final String task,
      final LocalDate startDate,
      final LocalDate endDate,
      final Integer priority,
      final Boolean isParentCollection) {
    log.debug("Retrieving Task by all Task fields - TaskManagerRepoImpl findByAllTaskFields");

    Query query = new Query();
    query.addCriteria(
        new Criteria()
            .andOperator(
                Criteria.where("task").is(task),
                Criteria.where("parent_id").is(parentId),
                Criteria.where("start_date").is(startDate),
                Criteria.where("end_date").is(endDate),
                Criteria.where("priority").is(priority),
                Criteria.where("isParentCollection").is(isParentCollection)));

    return mongoTemplate.find(query, Task.class);
  }
}
