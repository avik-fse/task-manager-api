package casestudy.taskmanager.controllers;

import casestudy.taskmanager.domains.ParentTask;
import casestudy.taskmanager.domains.Task;
import casestudy.taskmanager.repositories.ParentTaskRepository;
import casestudy.taskmanager.repositories.TaskManagerRepository;
import casestudy.taskmanager.repositories.TaskRepository;
import casestudy.taskmanager.service.TaskManagerService;
import casestudy.taskmanager.util.DateUtil;
import casestudy.taskmanager.util.SequenceGeneratorUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TaskManagerControllerTest {
  private final String baseUrl = "/api/taskManager";

  @Autowired private MockMvc mockMvc;

  private TaskManagerController taskManagerControllerToTest;

  private TaskManagerService taskManagerService;

  @Mock private TaskRepository taskRepository;
  @Mock private ParentTaskRepository parentTaskRepository;
  @Mock private TaskManagerRepository taskManagerRepository;
  @Mock private SequenceGeneratorUtil sequenceGeneratorUtil;
  @Mock private MongoTemplate mongoTemplate;

  @Autowired private DateUtil dateUtil;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    taskManagerService =
        new TaskManagerService(
            taskRepository,
            parentTaskRepository,
            taskManagerRepository,
            sequenceGeneratorUtil,
            mongoTemplate);
    taskManagerControllerToTest = new TaskManagerController(taskManagerService, dateUtil);

    mockMvc = MockMvcBuilders.standaloneSetup(taskManagerControllerToTest).build();

    doReturn(dummyTaskList()).when(taskRepository).findAll();
    doReturn(dummyParentTaskList()).when(parentTaskRepository).findAll();
    doCallRealMethod().when(taskManagerRepository).getTaskModelList(anyList(), anyList());

    doReturn(dummyTaskList()).when(taskRepository).findByTask(anyString());

    doReturn(dummyTaskList()).when(taskRepository).findByTaskId(anyLong());

    doReturn(dummyParentTaskList()).when(parentTaskRepository).findByParentId(anyLong());

    doReturn(dummyTaskList()).when(taskRepository).findByStartDate(any(LocalDate.class));

    doReturn(dummyTaskList()).when(taskRepository).findByEndDate(any(LocalDate.class));

    doReturn(dummyTaskList()).when(taskRepository).findByPriority(anyInt());

    doReturn(dummyParentTaskList()).when(taskManagerRepository).findByParentTaskName(anyString());

    doReturn(dummyTaskList())
        .when(taskManagerRepository)
        .findByAllTaskFields(
            anyLong(), anyString(), any(LocalDate.class), any(LocalDate.class), anyInt());

    doReturn(1l)
        .when(sequenceGeneratorUtil)
        .generateSequence(anyString(), any(MongoTemplate.class));

    doReturn(dummyTask()).when(taskRepository).save(any(Task.class));

    doReturn(dummyParentTask()).when(parentTaskRepository).save(any(ParentTask.class));
  }

  @Test
  public void getAllTasks() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/allTasks").accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].taskId").exists());
  }

  @Test
  public void getTaskByName() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/taskByName/Test Task")
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].taskId").value(1));
  }

  @Test
  public void getTaskById() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/taskById/1").accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.task").value("Test Task"));
  }

  @Test
  public void getTaskByStartDate() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/taskByStartDate/12-09-2019")
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].taskId").value(1));
  }

  @Test
  public void getTaskByEndDate() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/taskByEndDate/12-09-2019")
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].taskId").value(1));
  }

  @Test
  public void getTaskByPriority() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/taskByPriority/10")
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].taskId").value(1));
  }

  @Test
  public void getTaskByParentTask() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/taskByParentTask/Test Parent Task")
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].parentId").value("1"));
  }

  @Test
  public void addTask() throws Exception {
    String jsonStr =
        "{\n"
            + "\t\"task\" :\"test task\",\n"
            + "\t\"priority\" : \"1\",\n"
            + "\t\"startDate\":\"12-12-2019\"\n"
            + "}";
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(baseUrl + "/addTask")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(jsonStr))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(
            MockMvcResultMatchers.jsonPath("$.value")
                .value("Successfully added task to Database!"));
  }

  @Test
  public void updateTask() throws Exception {
    String jsonStr =
        "{\n"
            + "    \"taskId\": 1,\n"
            + "    \"task\": \"my first task\",\n"
            + "    \"priority\": 1,\n"
            + "    \"parentId\": 1,\n"
            + "    \"parentTask\": \"NA\",\n"
            + "    \"startDate\": \"03-05-2019\",\n"
            + "    \"endDate\": \"03-05-2019\"\n"
            + "}";
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(baseUrl + "/updateTask")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(jsonStr))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.value").value("Successfully updated task!"));
  }

  @Test
  public void searchTasks() throws Exception {
    String jsonStr =
        "{\n"
            + "    \"task\": \"Test Task\",\n"
            + "    \"priority\": 1,\n"
            + "    \"parentId\": 1,\n"
            + "    \"parentTask\": \"Test Parent Task\"\n"
            + "}";
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(baseUrl + "/search")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .content(jsonStr))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].taskId").exists());
  }

  @Test
  public void endTask() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(baseUrl + "/endTask/1").accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$.value").value("Successfully updated task!"));
  }

  private Task dummyTask() {
    Task task = new Task(1l, 1l, "Test Task", LocalDate.now(), LocalDate.now(), 15);
    task.setId("abc");

    return task;
  }

  private List<Task> dummyTaskList() {
    return Arrays.asList(dummyTask());
  }

  private ParentTask dummyParentTask() {
    ParentTask parentTask = new ParentTask(1l, "Test Parent Task");
    parentTask.setId("xyz");

    return parentTask;
  }

  private List<ParentTask> dummyParentTaskList() {
    return Arrays.asList(dummyParentTask());
  }
}
