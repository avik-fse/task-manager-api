package casestudy.taskmanager.controllers;

import casestudy.taskmanager.domains.ParentTask;
import casestudy.taskmanager.domains.Task;
import casestudy.taskmanager.repositories.ParentTaskRepository;
import casestudy.taskmanager.repositories.TaskManagerRepository;
import casestudy.taskmanager.repositories.TaskRepository;
import casestudy.taskmanager.service.TaskManagerService;
import casestudy.taskmanager.util.DateUtil;
import casestudy.taskmanager.util.SequenceGeneratorUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TaskManagerControllerTest {
  private final String baseUrl = "/api/taskManager";

  @Autowired private MockMvc mockMvc;

  @Mock private TaskRepository taskRepository;
  @Mock private ParentTaskRepository parentTaskRepository;
  @Mock private TaskManagerRepository taskManagerRepository;
  @Mock private SequenceGeneratorUtil sequenceGeneratorUtil;
  @Mock private MongoTemplate mongoTemplate;
  @Autowired private MessageSource messageSource;

  @Autowired private DateUtil dateUtil;

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    TaskManagerService taskManagerService =
        new TaskManagerService(
            taskRepository,
            parentTaskRepository,
            taskManagerRepository,
            sequenceGeneratorUtil,
            mongoTemplate,
            messageSource);
    TaskManagerController taskManagerControllerToTest =
        new TaskManagerController(taskManagerService, dateUtil);

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

    doReturn(dummyTaskList())
        .when(taskManagerRepository)
        .findByAllTaskFields(
            anyLong(),
            anyString(),
            any(LocalDate.class),
            any(LocalDate.class),
            anyInt(),
            anyBoolean());

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
  public void getAllParents() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/allParentsAndActiveTasks")
                .accept(MediaType.APPLICATION_JSON))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].parentId").exists());
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
  public void addTask() throws Exception {
    String jsonStr =
        "{\n"
            + "\t\"task\" :\"test task\",\n"
            + "\t\"priority\" : \"1\",\n"
            + "\t\"startDate\":\"12-12-2019\",\n"
            + "\t\"isParentCollection\":true\n"
            + "}";
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(baseUrl + "/addTask")
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .headers(getHttpHeaders("en"))
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
                .headers(getHttpHeaders("en"))
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

  @Test
  public void getI18nMessages() throws Exception {

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(baseUrl + "/i18nMessages").headers(getHttpHeaders("en")))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(MockMvcResultMatchers.jsonPath("$[0].key").exists());
  }

  private Task dummyTask() {
    Task task = new Task(1l, 1l, "Test Task", LocalDate.now(), LocalDate.now(), 15, false);
    task.setId("abc");
    task.setIsParentCollection(true);

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

  private HttpHeaders getHttpHeaders(String lang) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.add("Accept-Language", lang);
    httpHeaders.add("Content-Type", "application/json");

    return httpHeaders;
  }
}
