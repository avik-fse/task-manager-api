package casestudy.taskmanager.domains;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@Data
@Document(collection = "parent_task")
public class ParentTask {
  @Transient
  public static final String SEQUENCE_NAME = "parent_task_sequence";

  @Id private String id;

  @Field("parent_id")
  private Long parentId;

  @Field("parent_task")
  private String parentTask;

  public ParentTask(final Long parentId, final String parentTask) {
    this.parentId = parentId;
    this.parentTask = parentTask;
  }
}
