package casestudy.taskmanager.repositories;

import casestudy.taskmanager.domains.ParentTask;
import casestudy.taskmanager.domains.Task;
import casestudy.taskmanager.models.TaskModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Log4j2
@Repository
public class TaskManagerRepoImpl implements TaskManagerRepository {
  private final MongoTemplate mongoTemplate;
  private final ParentTaskRepository parentTaskRepository;
  private final TaskRepository taskRepository;

  public TaskManagerRepoImpl(
      final MongoTemplate mongoTemplate,
      final ParentTaskRepository parentTaskRepository,
      final TaskRepository taskRepository) {
    this.mongoTemplate = mongoTemplate;
    this.parentTaskRepository = parentTaskRepository;
    this.taskRepository = taskRepository;
  }

  @Override
  public List<TaskModel> findByParentTaskName(String parentTaskName) {
    List<TaskModel> taskModelList = null;
    log.debug("Retrieving Task by Parent Task - TaskManagerRepoImpl findByParentTaskName");
    List<ParentTask> parentTaskLst = parentTaskRepository.findByParentTask(parentTaskName);

    if (!CollectionUtils.isEmpty(parentTaskLst)) {
      // mongoTemplate.aggregate();
      List<Task> taskList = taskRepository.findByParentId(parentTaskLst.get(0).getParentId());

      taskModelList = getTaskModelList(parentTaskLst, taskList);
    } else {
      taskModelList = Collections.emptyList();
    }

    return taskModelList;
  }

  @Override
  public List<Task> findByAllTaskFields(
      Long parentId, String task, LocalDate startDate, LocalDate endDate, Integer priority) {
    log.debug("Retrieving Task by all Task fields - TaskManagerRepoImpl findByAllTaskFields");

    Query query = new Query();
    query.addCriteria(
        new Criteria()
            .andOperator(
                Criteria.where("task").is(task),
                Criteria.where("parent_id").is(parentId),
                Criteria.where("start_date").is(startDate),
                Criteria.where("end_date").is(endDate),
                Criteria.where("priority").is(priority)));

    return mongoTemplate.find(query, Task.class);
  }
}
