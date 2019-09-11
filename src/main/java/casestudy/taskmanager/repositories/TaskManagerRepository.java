package casestudy.taskmanager.repositories;

import casestudy.taskmanager.domains.ParentTask;
import casestudy.taskmanager.domains.Task;
import casestudy.taskmanager.models.TaskModel;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface TaskManagerRepository {
  List<TaskModel> findByParentTaskName(final String parentTaskName);

  List<Task> findByAllTaskFields(
      final Long parentId,
      final String task,
      final LocalDate startDate,
      final LocalDate endDate,
      final Integer priority);

  default List<TaskModel> getTaskModelList(
      final List<ParentTask> parentTaskList, final List<Task> taskList) {
    List<TaskModel> taskModelLst = new ArrayList<>();
    if (!CollectionUtils.isEmpty(taskList) && !CollectionUtils.isEmpty(parentTaskList)) {
      taskModelLst =
          taskList
              .parallelStream()
              .map(
                  task -> {
                    TaskModel taskModel = new TaskModel();
                    BeanUtils.copyProperties(task, taskModel);

                    // Set the Parent Task
                    Optional<ParentTask> parentTask =
                        parentTaskList.stream()
                            .filter(
                                parTask ->
                                    parTask.getParentId().longValue()
                                        == task.getParentId().longValue())
                            .findFirst();
                    if (parentTask.isPresent()) {
                      taskModel.setParentTask(parentTask.get().getParentTask());
                    }

                    return taskModel;
                  })
              .collect(Collectors.toList());
    }

    return taskModelLst;
  }
}
