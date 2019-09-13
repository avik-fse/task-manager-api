package casestudy.taskmanager.domains;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "database_sequences")
@Getter
@Setter
public class DatabaseSequence {
  @Id private String id;

  @Field("parent_task_sequence")
  private Long parentTaskSequence;

  @Field("task_sequence")
  private Long taskSequence;
}
