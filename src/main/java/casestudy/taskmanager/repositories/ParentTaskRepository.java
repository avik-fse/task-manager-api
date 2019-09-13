package casestudy.taskmanager.repositories;

import casestudy.taskmanager.domains.ParentTask;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ParentTaskRepository extends MongoRepository<ParentTask, Long> {
  String DEFAULT_PARENT_TASK = "NA";

  List<ParentTask> findByParentTask(final String parentTask);

  List<ParentTask> findByParentId(final Long parentId);
}
