package casestudy.taskmanager.domains;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@NoArgsConstructor
@Data
@Document(collection = "task")
public class Task {
  @Transient public static final String SEQUENCE_NAME = "task_sequence";

  @Id private String id;

  @Field("task_id")
  private Long taskId;

  @Field("parent_id")
  private Long parentId;

  @Field("task")
  private String task;

  @Field("start_date")
  private LocalDate startDate;

  @Field("end_date")
  private LocalDate endDate;

  @Field("priority")
  private Integer priority;

  @Field("isParentCollection")
  private Boolean isParentCollection;

  public Task(
      final Long taskId,
      final Long parentId,
      final String task,
      final LocalDate startDate,
      final LocalDate endDate,
      final Integer priority,
      final Boolean isParentCollection) {
    this.taskId = taskId;
    this.parentId = parentId;
    this.task = task;
    this.startDate = startDate;
    this.endDate = endDate;
    this.priority = priority;
    this.isParentCollection = isParentCollection;
  }
}
