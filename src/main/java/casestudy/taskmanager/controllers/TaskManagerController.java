package casestudy.taskmanager.controllers;

import casestudy.taskmanager.models.AppKeyValue;
import casestudy.taskmanager.models.TaskModel;
import casestudy.taskmanager.service.TaskManagerService;
import casestudy.taskmanager.util.DateUtil;
import lombok.extern.log4j.Log4j2;
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

  @GetMapping("/allParentsAndActiveTasks")
  public List<TaskModel> getAllParentsAndActiveTasks() {
    log.debug("Initiating TaskManagerController getAllParentsAndActiveTasks method");

    return taskManagerService.getAllParentsAndActiveTasks();
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

  @PostMapping("/addTask")
  public ResponseEntity<AppKeyValue> addTask(@RequestBody final TaskModel taskMode) {
    final String response = taskManagerService.addTask(taskMode);
    AppKeyValue respBody = new AppKeyValue("message", response);

    return new ResponseEntity<>(respBody, OK);
  }

  @PutMapping("/updateTask")
  public ResponseEntity<AppKeyValue> updateTask(@RequestBody final TaskModel taskMode) {
    final String response = taskManagerService.updateTask(taskMode);
    AppKeyValue respBody = new AppKeyValue("message", response);

    return new ResponseEntity<>(respBody, OK);
  }

  @PostMapping("/search")
  public List<TaskModel> searchTasks(@RequestBody final TaskModel taskModel) {

    return taskManagerService.searchTasks(taskModel);
  }

  @PutMapping("/endTask/{taskId}")
  public ResponseEntity<AppKeyValue> endTask(@PathVariable("taskId") final String taskId) {
    final String response = taskManagerService.endTask(taskId);
    AppKeyValue respBody = new AppKeyValue("message", response);

    return new ResponseEntity<>(respBody, OK);
  }

  @GetMapping("/i18nMessages")
  public List<AppKeyValue> getI18nMessages() {
    log.debug("Initiating TaskManagerController getI18nMessages method");

    return taskManagerService.getI18nMessages();
  }
}
