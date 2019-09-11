package casestudy.taskmanager.util;

import casestudy.taskmanager.domains.DatabaseSequence;
import casestudy.taskmanager.domains.ParentTask;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;
import static org.springframework.data.mongodb.core.query.Criteria.where;
import static org.springframework.data.mongodb.core.query.Query.query;

@Component
@Log4j2
public class SequenceGeneratorUtil {
  public Long generateSequence(final String seqName, final MongoTemplate mongoTemplate) {
    log.debug("Generating sequence for {}", seqName);

    DatabaseSequence counter =
        mongoTemplate.findAndModify(
            query(where("_id").is(seqName)),
            new Update().inc(seqName, 1),
            options().returnNew(true).upsert(true),
            DatabaseSequence.class);
    return !Objects.isNull(counter)
        ? (StringUtils.equals(seqName, ParentTask.SEQUENCE_NAME)
            ? counter.getParentTaskSequence()
            : counter.getTaskSequence())
        : 1;
  }
}
