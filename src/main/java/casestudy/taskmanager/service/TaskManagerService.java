package casestudy.taskmanager.service;

import casestudy.taskmanager.domains.ParentTask;
import casestudy.taskmanager.domains.Task;
import casestudy.taskmanager.exception.DBException;
import casestudy.taskmanager.exception.TaskValidationException;
import casestudy.taskmanager.models.AppKeyValue;
import casestudy.taskmanager.models.TaskModel;
import casestudy.taskmanager.repositories.ParentTaskRepository;
import casestudy.taskmanager.repositories.TaskManagerRepository;
import casestudy.taskmanager.repositories.TaskRepository;
import casestudy.taskmanager.util.MessageKeyUtil;
import casestudy.taskmanager.util.SequenceGeneratorUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
  private final MessageSource messageSource;

  public TaskManagerService(
      final TaskRepository taskRepository,
      final ParentTaskRepository parentTaskRepository,
      final TaskManagerRepository taskManagerRepository,
      final SequenceGeneratorUtil sequenceGeneratorUtil,
      final MongoTemplate mongoTemplate,
      final MessageSource messageSource) {
    this.taskRepository = taskRepository;
    this.parentTaskRepository = parentTaskRepository;
    this.taskManagerRepository = taskManagerRepository;
    this.sequenceGeneratorUtil = sequenceGeneratorUtil;
    this.mongoTemplate = mongoTemplate;
    this.messageSource = messageSource;
  }

  public List<TaskModel> getAllTasks() {
    log.debug("Processing TaskManagerService getAllTasks");

    List<Task> allTasks = taskRepository.findAll();
    List<ParentTask> allParentTasks = parentTaskRepository.findAll();

    return taskManagerRepository.getTaskModelList(allParentTasks, allTasks);
  }

  public List<TaskModel> getAllParentsAndActiveTasks() {
    log.debug("Processing TaskManagerService getAllParentsAndActiveTasks");

    List<ParentTask> allParentTasks = parentTaskRepository.findAll();

    List<TaskModel> allParentTaskList =
        allParentTasks
            .parallelStream()
            .map(
                parTsk -> {
                  TaskModel taskModel = new TaskModel();
                  BeanUtils.copyProperties(parTsk, taskModel);
                  taskModel.setIsParentCollection(true);

                  return taskModel;
                })
            .collect(Collectors.toList());

    List<TaskModel> allTaskList = getAllTasks();

    allParentTaskList.addAll(
        allTaskList.stream().filter(task -> task.getPriority() >= 0).collect(Collectors.toList()));

    return allParentTaskList;
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

  public String addTask(final TaskModel taskModel) {
    String result;

    log.debug("Processing TaskManagerService addTask");

    if (validateRequest(taskModel, false)) {
      log.debug("Check if Parent Task is present");

      // Check if the parentTask is empty then set parentTask as NA
      if (StringUtils.isBlank(taskModel.getParentTask())) {
        taskModel.setParentTask(DEFAULT_PARENT_TASK);
      }

      // Parse the parentTask
      boolean existingParentTask =
          taskModel.getIsParentCollection() == null ? false : taskModel.getIsParentCollection();
      Long existingParentTaskId = taskModel.getParentId() == null ? 0 : taskModel.getParentId();
      List<ParentTask> parentTaskLst = null;
      List<Task> taskList = null;
      if (existingParentTask && existingParentTaskId > 0) {
        parentTaskLst = parentTaskRepository.findByParentId(existingParentTaskId);
      } else if (!existingParentTask && existingParentTaskId > 0) {
        taskList = taskRepository.findByTaskId(existingParentTaskId);
      } else {
        parentTaskLst = parentTaskRepository.findByParentTask(taskModel.getParentTask());
      }

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
                taskModel.getPriority(),
                taskModel.getIsParentCollection());

        if (!CollectionUtils.isEmpty(matchingTask)) {
          log.error("Below task already exists\n{}", matchingTask.get(0));

          throw new DBException(getMessage("exception.taskExist"));
        } else {
          log.debug("Task not present, proceeding to add task");

          Task newTask =
              saveTask(
                  parentTask.getParentId(),
                  taskModel.getTask(),
                  taskModel.getStartDate(),
                  taskModel.getEndDate(),
                  taskModel.getPriority(),
                  taskModel.getIsParentCollection(),
                  null);

          if (newTask != null && StringUtils.isNotBlank(newTask.getId())) {
            result = getMessage("success.addTask");
          } else {
            result = getMessage("exception.addTask");

            throw new DBException(result);
          }
        }
      } else if (!CollectionUtils.isEmpty(taskList)) {
        // Task as parent flow
        Task parentTask = taskList.get(0);

        log.debug("Found below matching task\n{}", parentTask);

        // Find if the input task is already present by matching all fields
        List<Task> matchingTask =
            taskManagerRepository.findByAllTaskFields(
                parentTask.getParentId(),
                taskModel.getTask(),
                taskModel.getStartDate(),
                taskModel.getEndDate(),
                taskModel.getPriority(),
                taskModel.getIsParentCollection());

        if (!CollectionUtils.isEmpty(matchingTask)) {
          log.error("Below task already exists\n{}", matchingTask.get(0));

          throw new DBException(getMessage("exception.taskExist"));
        } else {
          log.debug("Task not present, proceeding to add task");

          Task newTask =
              saveTask(
                  parentTask.getParentId(),
                  taskModel.getTask(),
                  taskModel.getStartDate(),
                  taskModel.getEndDate(),
                  taskModel.getPriority(),
                  taskModel.getIsParentCollection(),
                  null);

          if (newTask != null && StringUtils.isNotBlank(newTask.getId())) {
            result = getMessage("success.addTask");
          } else {
            result = getMessage("exception.addTask");

            throw new DBException(result);
          }
        }
      } else {
        log.debug("Parent Task not present, so add Parent Task");

        ParentTask parentTaskPostSave = saveParentTask(taskModel.getParentTask(), null);

        if (parentTaskPostSave != null && StringUtils.isNotBlank(parentTaskPostSave.getId())) {
          // Prepare the new ParentTask object to be inserted in DB
          taskModel.setIsParentCollection(true);
          Task newTask =
              saveTask(
                  parentTaskPostSave.getParentId(),
                  taskModel.getTask(),
                  taskModel.getStartDate(),
                  taskModel.getEndDate(),
                  taskModel.getPriority(),
                  taskModel.getIsParentCollection(),
                  null);

          if (newTask != null && StringUtils.isNotBlank(newTask.getId())) {
            result = getMessage("success.addTask");
          } else {
            result = getMessage("exception.addTask");
            throw new DBException(result);
          }

        } else {
          final String errMsg = getMessage("exception.addParent");
          log.error(errMsg);

          throw new DBException(errMsg);
        }
      }
    } else {
      log.error(getMessage("error.invalidRequest"));

      throw new TaskValidationException(getMessage("error.invalidRequest"));
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

      // Parse the parentTask
      boolean hasParentInParent =
          taskModel.getIsParentCollection() == null ? false : taskModel.getIsParentCollection();
      Long existingParentTaskId = taskModel.getParentId() == null ? 0 : taskModel.getParentId();
      List<ParentTask> parentTaskLst = null;
      List<Task> taskAsParentList = null;
      if (hasParentInParent && existingParentTaskId > 0) {
        parentTaskLst = parentTaskRepository.findByParentId(existingParentTaskId);
      } else if (!hasParentInParent && existingParentTaskId > 0) {
        taskAsParentList = taskRepository.findByTaskId(existingParentTaskId);
      } else {
        parentTaskLst = parentTaskRepository.findByParentTask(taskModel.getParentTask());
      }

      if (!CollectionUtils.isEmpty(parentTaskLst) || !CollectionUtils.isEmpty(taskAsParentList)) {
        // Retrieve the Task from DB
        List<Task> taskList = taskRepository.findByTaskId(taskModel.getTaskId());

        if (!CollectionUtils.isEmpty(taskList)) {
          // Validate if task is editable
          Task task = taskList.get(0);

          // If the priority of the task is set to -1 then that means it has ended and cannot be
          // updated
          if (task.getPriority() == -1) {
            String errMsg = getMessage("exception.updateTaskEnded");
            log.error(errMsg);

            throw new DBException(errMsg);
          } else {
            // Proceed with update
            if (hasParentInParent) {

              ParentTask parentTask = parentTaskLst.get(0);
              // Update Parent Task only if the text hsa changed for the same parent id is different
              if (!StringUtils.equals(taskModel.getParentTask(), parentTask.getParentTask())
                  && taskModel.getParentId().longValue() == parentTask.getParentId().longValue()) {
                saveParentTask(taskModel.getParentTask(), parentTask);
                recordUpdated = true;
              } else {
                log.debug("Ignoring Update of Parent Task as the fields are identical");
              }
            } else {
              Task parentTask = taskAsParentList.get(0);
              // Update Parent Task only if the text hsa changed for the same parent id is different
              if (!StringUtils.equals(taskModel.getParentTask(), parentTask.getTask())
                  && taskModel.getParentId().longValue() == parentTask.getParentId().longValue()) {
                // Update the task name and id representing the parent task
                saveTask(
                    parentTask.getParentId(),
                    taskModel.getParentTask(),
                    parentTask.getStartDate(),
                    parentTask.getEndDate(),
                    parentTask.getPriority(),
                    parentTask.getIsParentCollection(),
                    parentTask);
                recordUpdated = true;
              } else {
                log.debug("Ignoring Update of Parent Task as the fields are identical");
              }
            }

            // Update Task only if the input is different
            if (!StringUtils.equals(taskModel.getTask(), task.getTask())
                || taskModel.getParentId() != task.getParentId()
                || !taskModel.getStartDate().isEqual(task.getStartDate())
                || (taskModel.getEndDate() != null
                    && task.getEndDate() != null
                    && !taskModel.getEndDate().isEqual(task.getEndDate()))
                || (taskModel.getEndDate() != null && task.getEndDate() == null)
                || taskModel.getPriority() != task.getPriority()
                || taskModel.getIsParentCollection() != task.getIsParentCollection()) {
              saveTask(
                  taskModel.getParentId(),
                  taskModel.getTask(),
                  taskModel.getStartDate(),
                  taskModel.getEndDate(),
                  taskModel.getPriority(),
                  taskModel.getIsParentCollection(),
                  task);

              recordUpdated = true;

            } else {
              log.debug("Ignoring Update of Task as the fields are identical");
            }
          }

        } else {
          String errMsg =
              MessageFormat.format(getMessage("exception.taskNotFoundById"), taskModel.getTaskId());
          log.error(errMsg);

          throw new DBException(errMsg);
        }

      } else {
        String errMsg =
            MessageFormat.format(
                getMessage("exception.parentTaskNotFoundById"), taskModel.getParentId());
        log.error(errMsg);

        throw new DBException(errMsg);
      }
    } else {
      log.error(getMessage("error.invalidRequest"));

      throw new TaskValidationException(getMessage("error.invalidRequest"));
    }

    if (recordUpdated) {
      result = getMessage("success.updateTask");
    } else {
      result = getMessage("error.ignoreUpdate");
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
      final Boolean isParentCollection,
      Task taskObj) {

    final boolean isExistingRecord = (taskObj != null);

    // Prepare the new ParentTask object to be inserted in DB
    if (isExistingRecord) {
      taskObj.setTask(task);
      taskObj.setStartDate(startDate);
      taskObj.setEndDate(endDate);
      taskObj.setPriority(priority);
      taskObj.setParentId(parentId);
      taskObj.setIsParentCollection(isParentCollection);
    } else {
      taskObj =
          new Task(
              sequenceGeneratorUtil.generateSequence(Task.SEQUENCE_NAME, mongoTemplate),
              parentId,
              task,
              startDate,
              endDate,
              priority,
              isParentCollection);
    }

    log.debug(
        "{} below Task to task collection\n{}", isExistingRecord ? "Updating" : "Adding", taskObj);

    Task taskPostSave = taskRepository.save(taskObj);
    if (taskPostSave != null && StringUtils.isNotBlank(taskPostSave.getId())) {
      log.debug("Successfully {} Task to DB", isExistingRecord ? "updated" : "added");
    } else {
      final String errMsg = getMessage("exception.saveTask");
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
      final String errMsg = getMessage("exception.saveParentTask");
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
                  throw new TaskValidationException(getMessage("exception.priorityValidation"));
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
        result = getMessage("error.taskNotFound");
      }
    } else {
      throw new TaskValidationException(getMessage("error.invalidTaskId"));
    }

    return result;
  }

  public TaskModel getTaskById(String taskId) {
    TaskModel result = new TaskModel();
    if (NumberUtils.isCreatable(taskId)) {
      List<Task> taskList = taskRepository.findByTaskId(Long.parseLong(taskId));

      if (!CollectionUtils.isEmpty(taskList)) {
        Task matchedTask = taskList.get(0);
        final boolean isParentCollection = matchedTask.getIsParentCollection();
        List<ParentTask> parentTaskList = null;
        List<Task> taskParentLst = null;

        if (isParentCollection) {
          parentTaskList = parentTaskRepository.findByParentId(matchedTask.getParentId());
        } else {
          taskParentLst = taskRepository.findByTaskId(matchedTask.getParentId());
        }

        if (!CollectionUtils.isEmpty(parentTaskList) && isParentCollection) {
          ParentTask matchedParentTask = parentTaskList.get(0);
          BeanUtils.copyProperties(matchedTask, result);
          BeanUtils.copyProperties(matchedParentTask, result);
        } else if (!CollectionUtils.isEmpty(taskParentLst) && !isParentCollection) {
          Task matchedParentTask = taskParentLst.get(0);
          BeanUtils.copyProperties(matchedTask, result);
          result.setParentTask(matchedParentTask.getTask());
        } else {
          throw new DBException(getMessage("exception.parentTaskNotFound"));
        }

      } else {
        throw new TaskValidationException(getMessage("exception.taskNotFound"));
      }
    } else {
      throw new TaskValidationException(getMessage("error.invalidTaskId"));
    }

    return result;
  }

  private String getMessage(final String msgKey) {
    return messageSource.getMessage(msgKey, null, LocaleContextHolder.getLocale());
  }

  public List<AppKeyValue> getI18nMessages() {
    List<String> keys = MessageKeyUtil.getAppKeys();

    List<AppKeyValue> appMessageList =
        keys.stream()
            .map(
                key -> {
                  final String value = getMessage(key);
                  AppKeyValue keyValuePair = new AppKeyValue(key, value);

                  return keyValuePair;
                })
            .collect(Collectors.toList());

    return appMessageList;
  }
}
