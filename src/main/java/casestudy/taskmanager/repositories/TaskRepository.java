package casestudy.taskmanager.repositories;

import casestudy.taskmanager.domains.Task;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends MongoRepository<Task, Long> {
  List<Task> findByTaskId(final Long taskId);

  List<Task> findByTask(final String task);

  List<Task> findByStartDate(final LocalDate startDate);

  List<Task> findByEndDate(final LocalDate endDate);

  List<Task> findByPriority(final Integer priority);

  List<Task> findByParentId(final Long parentId);
}
