package casestudy.taskmanager.controllers;

import casestudy.taskmanager.models.TaskModel;
import casestudy.taskmanager.service.TaskManagerService;
import casestudy.taskmanager.util.DateUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/api/taskManager")
@Log4j2
public class TaskManagerController {
  private final TaskManagerService taskManagerService;
  private final DateUtil dateUtil;

  public TaskManagerController(
      final TaskManagerService taskManagerService, final DateUtil dateUtil) {
    this.taskManagerService = taskManagerService;
    this.dateUtil = dateUtil;
  }

  @GetMapping("/allTasks")
  public List<TaskModel> getAllTasks() {
    log.debug("Initiating TaskManagerController getAllTasks method");

    return taskManagerService.getAllTasks();
  }

  @GetMapping("/taskByName/{taskName}")
  public List<TaskModel> getTaskByName(@PathVariable final String taskName) {
    log.debug("Initiating TaskManagerController getTaskByName method");

    return taskManagerService.getTaskByName(taskName);
  }

  @GetMapping("/taskById/{taskId}")
  public TaskModel getTaskById(@PathVariable final String taskId) {
    log.debug("Initiating TaskManagerController getTaskById method");

    return taskManagerService.getTaskById(taskId);
  }

  @GetMapping("/taskByStartDate/{startDate}")
  public List<TaskModel> getTaskByStartDate(@PathVariable("startDate") final String startDateStr) {
    log.debug("Initiating TaskManagerController getTaskByStartDate method");

    return taskManagerService.getTaskByStartDate(dateUtil.toLocalDate(startDateStr));
  }

  @GetMapping("/taskByEndDate/{endDate}")
  public List<TaskModel> getTaskByEndDate(@PathVariable("endDate") final String endDateStr) {
    log.debug("Initiating TaskManagerController getTaskByEndDate method");

    return taskManagerService.getTaskByEndDate(dateUtil.toLocalDate(endDateStr));
  }

  @GetMapping("/taskByPriority/{priority}")
  public List<TaskModel> getTaskByPriority(@PathVariable("priority") final String priority) {
    log.debug("Initiating TaskManagerController getTaskByEndDate method");

    return taskManagerService.getTaskByPriority(priority);
  }

  @GetMapping("/taskByParentTask/{parentTask}")
  public List<TaskModel> getTaskByParentTask(@PathVariable("parentTask") final String parentTask) {
    log.debug("Initiating TaskManagerController getTaskByParentTask method");

    return taskManagerService.getTaskByParentTask(parentTask);
  }

  @PostMapping("/addTask")
  public ResponseEntity<KeyValuePair> addTask(@RequestBody final TaskModel taskMode) {
    final String response = taskManagerService.addTask(taskMode);
    KeyValuePair respBody = new KeyValuePair("message", response);

    return new ResponseEntity<>(respBody, OK);
  }

  @PutMapping("/updateTask")
  public ResponseEntity<KeyValuePair> updateTask(@RequestBody final TaskModel taskMode) {
    final String response = taskManagerService.updateTask(taskMode);
    KeyValuePair respBody = new KeyValuePair("message", response);

    return new ResponseEntity<>(respBody, OK);
  }

  @PostMapping("/search")
  public List<TaskModel> searchTasks(@RequestBody final TaskModel taskModel) {

    return taskManagerService.searchTasks(taskModel);
  }

  @PutMapping("/endTask/{taskId}")
  public ResponseEntity<KeyValuePair> endTask(@PathVariable("taskId") final String taskId) {
    final String response = taskManagerService.endTask(taskId);
    KeyValuePair respBody = new KeyValuePair("message", response);

    return new ResponseEntity<>(respBody, OK);
  }
}
