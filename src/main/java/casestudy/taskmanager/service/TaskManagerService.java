package casestudy.taskmanager.service;

import casestudy.taskmanager.domains.ParentTask;
import casestudy.taskmanager.domains.Task;
import casestudy.taskmanager.exception.DBException;
import casestudy.taskmanager.exception.TaskValidationException;
import casestudy.taskmanager.models.TaskModel;
import casestudy.taskmanager.repositories.ParentTaskRepository;
import casestudy.taskmanager.repositories.TaskManagerRepository;
import casestudy.taskmanager.repositories.TaskRepository;
import casestudy.taskmanager.util.SequenceGeneratorUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static casestudy.taskmanager.repositories.ParentTaskRepository.DEFAULT_PARENT_TASK;

@Service
@Log4j2
public class TaskManagerService {
  private final TaskRepository taskRepository;
  private final ParentTaskRepository parentTaskRepository;
  private final TaskManagerRepository taskManagerRepository;
  private final SequenceGeneratorUtil sequenceGeneratorUtil;
  private final MongoTemplate mongoTemplate;

  public TaskManagerService(
      final TaskRepository taskRepository,
      final ParentTaskRepository parentTaskRepository,
      final TaskManagerRepository taskManagerRepository,
      final SequenceGeneratorUtil sequenceGeneratorUtil,
      final MongoTemplate mongoTemplate) {
    this.taskRepository = taskRepository;
    this.parentTaskRepository = parentTaskRepository;
    this.taskManagerRepository = taskManagerRepository;
    this.sequenceGeneratorUtil = sequenceGeneratorUtil;
    this.mongoTemplate = mongoTemplate;
  }

  public List<TaskModel> getAllTasks() {
    log.debug("Processing TaskManagerService getAllTasks");

    List<Task> allTasks = taskRepository.findAll();
    List<ParentTask> allParentTasks = parentTaskRepository.findAll();

    return taskManagerRepository.getTaskModelList(allParentTasks, allTasks);
  }

  public List<TaskModel> getTaskByName(final String taskName) {
    log.debug("Processing TaskManagerService getTaskByName");

    List<Task> taskList = taskRepository.findByTask(taskName);
    List<ParentTask> allParentTasks = parentTaskRepository.findAll();

    return taskManagerRepository.getTaskModelList(allParentTasks, taskList);
  }

  public List<TaskModel> getTaskByStartDate(final LocalDate startDate) {
    log.debug("Processing TaskManagerService getTaskByStartDate({})", startDate);

    List<Task> taskList = taskRepository.findByStartDate(startDate);
    List<ParentTask> allParentTasks = parentTaskRepository.findAll();

    return taskManagerRepository.getTaskModelList(allParentTasks, taskList);
  }

  public List<TaskModel> getTaskByEndDate(final LocalDate endDate) {
    log.debug("Processing TaskManagerService getTaskByEndDate({})", endDate);

    List<Task> taskList = taskRepository.findByEndDate(endDate);
    List<ParentTask> allParentTasks = parentTaskRepository.findAll();

    return taskManagerRepository.getTaskModelList(allParentTasks, taskList);
  }

  public List<TaskModel> getTaskByPriority(String priority) {
    log.debug("Processing TaskManagerService getTaskByPriority({})", priority);

    List<Task> taskList = taskRepository.findByPriority(Integer.parseInt(priority));
    List<ParentTask> allParentTasks = parentTaskRepository.findAll();

    return taskManagerRepository.getTaskModelList(allParentTasks, taskList);
  }

  public List<TaskModel> getTaskByParentTask(String parentTask) {
    log.debug("Processing TaskManagerService getTaskByParentTask({})", parentTask);

    return taskManagerRepository.findByParentTaskName(parentTask);
  }

  public String addTask(final TaskModel taskModel) {
    String result;

    log.debug("Processing TaskManagerService addTask");

    if (validateRequest(taskModel, false)) {
      log.debug("Check if Parent Task is present");

      // Check if the parentTask is empty then set parentTask as NA
      if (StringUtils.isBlank(taskModel.getParentTask())) {
        taskModel.setParentTask(DEFAULT_PARENT_TASK);
      }

      List<ParentTask> parentTaskLst =
          parentTaskRepository.findByParentTask(taskModel.getParentTask());

      if (!CollectionUtils.isEmpty(parentTaskLst)) {
        ParentTask parentTask = parentTaskLst.get(0);

        log.debug("Found below matching parent task\n{}", parentTask);

        // Find if the input task is already present by matching all fields
        List<Task> matchingTask =
            taskManagerRepository.findByAllTaskFields(
                parentTask.getParentId(),
                taskModel.getTask(),
                taskModel.getStartDate(),
                taskModel.getEndDate(),
                taskModel.getPriority());

        if (!CollectionUtils.isEmpty(matchingTask)) {
          log.error("Below task already exists\n{}", matchingTask.get(0));

          throw new DBException("Task already exists");
        } else {
          log.debug("Task not present, proceeding to add task");

          Task newTask =
              saveTask(
                  parentTask.getParentId(),
                  taskModel.getTask(),
                  taskModel.getStartDate(),
                  taskModel.getEndDate(),
                  taskModel.getPriority(),
                  null);

          if (newTask != null && StringUtils.isNotBlank(newTask.getId())) {
            result = "Successfully added task to Database!";
          } else {
            result = "Failed to add task to Database.";
            throw new DBException(result);
          }
        }
      } else {
        log.debug("Parent Task not present, so add Parent Task");

        ParentTask parentTaskPostSave = saveParentTask(taskModel.getParentTask(), null);

        if (parentTaskPostSave != null && StringUtils.isNotBlank(parentTaskPostSave.getId())) {
          // Prepare the new ParentTask object to be inserted in DB
          Task newTask =
              saveTask(
                  parentTaskPostSave.getParentId(),
                  taskModel.getTask(),
                  taskModel.getStartDate(),
                  taskModel.getEndDate(),
                  taskModel.getPriority(),
                  null);

          if (newTask != null && StringUtils.isNotBlank(newTask.getId())) {
            result = "Successfully added task to Database!";
          } else {
            result = "Failed to add task to Database.";
            throw new DBException(result);
          }

        } else {
          final String errMsg = "Failed to save Parent Task";
          log.error(errMsg);

          throw new DBException(errMsg);
        }
      }
    } else {
      log.error("Invalid request");

      throw new TaskValidationException("Invalid request");
    }

    return result;
  }

  public String updateTask(final TaskModel taskModel) {
    String result;
    boolean recordUpdated = false;

    log.debug("Processing TaskManagerService updateTask");

    if (validateRequest(taskModel, true)) {
      log.debug("Check if Parent Task is present");

      // Check if the parentTask is empty then set parentTask as NA
      if (StringUtils.isBlank(taskModel.getParentTask())) {
        taskModel.setParentTask(DEFAULT_PARENT_TASK);
      }

      // Retrieve the parentTask from DB
      List<ParentTask> parentTaskLst = parentTaskRepository.findByParentId(taskModel.getParentId());

      if (!CollectionUtils.isEmpty(parentTaskLst)) {
        // Retrieve the Task from DB
        List<Task> taskList = taskRepository.findByTaskId(taskModel.getTaskId());

        if (!CollectionUtils.isEmpty(taskList)) {
          // Validate if task is editable
          Task task = taskList.get(0);

          // If the priority of the task is set to -1 then that means it has ended and cannot be
          // updated
          if (task.getPriority() == -1) {
            String errMsg = "Task has already ended and cannot be updated";
            log.error(errMsg);

            throw new DBException(errMsg);
          } else {
            // Proceed with update
            ParentTask parentTask = parentTaskLst.get(0);
            // Update Parent Task only if the input is different
            if (!StringUtils.equals(taskModel.getParentTask(), parentTask.getParentTask())) {
              saveParentTask(taskModel.getParentTask(), parentTask);
              recordUpdated = true;
            } else {
              log.debug("Ignoring Update of Parent Task as the fields are identical");
            }

            // Update Task only if the input is different
            if (!StringUtils.equals(taskModel.getTask(), task.getTask())
                || taskModel.getParentId() != task.getParentId()
                || !taskModel.getStartDate().isEqual(task.getStartDate())
                || (taskModel.getEndDate() != null
                    && task.getEndDate() != null
                    && !taskModel.getEndDate().isEqual(task.getEndDate()))
                || (taskModel.getEndDate() != null && task.getEndDate() == null)
                || taskModel.getPriority() != task.getPriority()) {
              saveTask(
                  parentTask.getParentId(),
                  taskModel.getTask(),
                  taskModel.getStartDate(),
                  taskModel.getEndDate(),
                  taskModel.getPriority(),
                  task);

              recordUpdated = true;

            } else {
              log.debug("Ignoring Update of Task as the fields are identical");
            }
          }

        } else {
          String errMsg =
              MessageFormat.format("Could not find task with id: {0}", taskModel.getTaskId());
          log.error(errMsg);

          throw new DBException(errMsg);
        }

      } else {
        String errMsg =
            MessageFormat.format(
                "Could not find parent task with id: {0}", taskModel.getParentId());
        log.error(errMsg);

        throw new DBException(errMsg);
      }
    } else {
      log.error("Invalid request");

      throw new TaskValidationException("Invalid request");
    }

    if (recordUpdated) {
      result = "Successfully updated task!";
    } else {
      result = "Ignored update as records did not change!";
    }
    return result;
  }

  private boolean validateRequest(final TaskModel taskModel, final boolean validateIds) {
    boolean isValidRequest = false;
    if (StringUtils.isNoneBlank(taskModel.getTask())
        && taskModel.getPriority() >= -1
        && taskModel.getPriority() <= 30
        && taskModel.getStartDate() != null) {

      if (validateIds && taskModel.getTaskId() > 0 && taskModel.getParentId() > 0) {
        isValidRequest = true;
      } else if (!validateIds) {
        isValidRequest = true;
      }

      if (isValidRequest && taskModel.getStartDate() != null && taskModel.getEndDate() != null) {
        if (taskModel.getStartDate().isAfter(taskModel.getEndDate())) {
          isValidRequest = false;
        }
      }
    }
    return isValidRequest;
  }

  private Task saveTask(
      final Long parentId,
      final String task,
      final LocalDate startDate,
      final LocalDate endDate,
      final Integer priority,
      Task taskObj) {

    final boolean isExistingRecord = (taskObj != null);

    // Prepare the new ParentTask object to be inserted in DB
    if (isExistingRecord) {
      taskObj.setTask(task);
      taskObj.setStartDate(startDate);
      taskObj.setEndDate(endDate);
      taskObj.setPriority(priority);
    } else {
      taskObj =
          new Task(
              sequenceGeneratorUtil.generateSequence(Task.SEQUENCE_NAME, mongoTemplate),
              parentId,
              task,
              startDate,
              endDate,
              priority);
    }

    log.debug(
        "{} below Task to task collection\n{}", isExistingRecord ? "Updating" : "Adding", taskObj);

    Task taskPostSave = taskRepository.save(taskObj);
    if (taskPostSave != null && StringUtils.isNotBlank(taskPostSave.getId())) {
      log.debug("Successfully {} Task to DB", isExistingRecord ? "updated" : "added");
    } else {
      final String errMsg = "Failed to save Task";
      log.error(errMsg);

      throw new DBException(errMsg);
    }

    return taskPostSave;
  }

  private ParentTask saveParentTask(final String parentTask, ParentTask parentTaskObj) {
    final boolean isExtingRecord = (parentTaskObj != null);

    // Prepare the new ParentTask object to be inserted in DB
    if (isExtingRecord) {
      parentTaskObj.setParentTask(parentTask);
    } else {
      parentTaskObj =
          new ParentTask(
              sequenceGeneratorUtil.generateSequence(ParentTask.SEQUENCE_NAME, mongoTemplate),
              parentTask);
    }

    log.debug(
        "{} below Parent Task to parent_task collection\n{}",
        isExtingRecord ? "Updating" : "Adding",
        parentTaskObj);
    ParentTask parentTaskPostSave = parentTaskRepository.save(parentTaskObj);

    if (parentTaskPostSave != null && StringUtils.isNotBlank(parentTaskPostSave.getId())) {
      log.debug("Successfully {} ParentTask to DB", isExtingRecord ? "updated" : "added");
    } else {
      final String errMsg = "Failed to save ParentTask";
      log.error(errMsg);

      throw new DBException(errMsg);
    }

    return parentTaskPostSave;
  }

  public List<TaskModel> searchTasks(TaskModel taskModel) {
    List<TaskModel> allTasks = getAllTasks();

    return Optional.ofNullable(allTasks).orElseGet(Collections::emptyList).stream()
        .filter(
            task -> {
              boolean matched;
              if (StringUtils.isNotBlank(taskModel.getTask())) {
                matched =
                    StringUtils.equalsAnyIgnoreCase(taskModel.getTask().trim(), task.getTask());
              } else {
                // skip filter
                matched = true;
              }

              return matched;
            })
        .filter(
            task -> {
              boolean matched;
              if (StringUtils.isNotBlank(taskModel.getParentTask())) {
                matched =
                    StringUtils.equalsAnyIgnoreCase(
                        taskModel.getParentTask().trim(), task.getParentTask());
              } else {
                // skip filter
                matched = true;
              }

              return matched;
            })
        .filter(
            task -> {
              boolean matched;

              if (taskModel.getPriorityFrom() == null) {
                taskModel.setPriorityFrom(0);
              }
              if (taskModel.getPriorityTo() == null) {
                taskModel.setPriorityTo(0);
              }
              if (taskModel.getPriorityFrom() > 0 && taskModel.getPriorityTo() > 0) {
                if (taskModel.getPriorityFrom() > taskModel.getPriorityTo()) {
                  throw new TaskValidationException("PriorityFrom is greater than PriorityTo");
                }
                matched =
                    task.getPriority() >= taskModel.getPriorityFrom()
                        && task.getPriority() <= taskModel.getPriorityTo();
              } else {
                // skip filter
                matched = true;
              }

              return matched;
            })
        .filter(
            task -> {
              boolean matched;
              if (taskModel.getStartDate() != null) {
                matched = task.getStartDate().isEqual(taskModel.getStartDate());
              } else {
                // skip filter
                matched = true;
              }

              return matched;
            })
        .filter(
            task -> {
              boolean matched;
              if (taskModel.getEndDate() != null) {
                matched = task.getEndDate().isEqual(taskModel.getEndDate());
              } else {
                // skip filter
                matched = true;
              }

              return matched;
            })
        .collect(Collectors.toList());
  }

  public String endTask(String taskId) {
    String result;

    if (NumberUtils.isCreatable(taskId)) {
      List<Task> taskList = taskRepository.findByTaskId(Long.parseLong(taskId));
      if (!CollectionUtils.isEmpty(taskList)) {
        Task task = taskList.get(0);

        TaskModel taskModel = new TaskModel();
        BeanUtils.copyProperties(task, taskModel);
        taskModel.setPriority(-1);
        result = updateTask(taskModel);
      } else {
        result = "No task found";
      }
    } else {
      throw new TaskValidationException("Invalid taskId");
    }

    return result;
  }

  public TaskModel getTaskById(String taskId) {
    TaskModel result = new TaskModel();
    if (NumberUtils.isCreatable(taskId)) {
      List<Task> taskList = taskRepository.findByTaskId(Long.parseLong(taskId));

      if (!CollectionUtils.isEmpty(taskList)) {
        Task matchedTask = taskList.get(0);
        List<ParentTask> parentTaskList =
            parentTaskRepository.findByParentId(matchedTask.getParentId());

        if (!CollectionUtils.isEmpty(parentTaskList)) {
          ParentTask matchedParentTask = parentTaskList.get(0);
          BeanUtils.copyProperties(matchedTask, result);
          BeanUtils.copyProperties(matchedParentTask, result);
        } else {
          throw new DBException("Parent Task not found. Invalid record");
        }

      } else {
        throw new TaskValidationException("Task not found");
      }
    } else {
      throw new TaskValidationException("Invalid taskId");
    }

    return result;
  }
}
