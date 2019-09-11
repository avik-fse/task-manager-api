package casestudy.taskmanager.util;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@Log4j2
public class DateUtil {

  public LocalDate toLocalDate(final String dateString) {
    log.debug("Attempting to convert date string {} to LocalDate", dateString);
    return LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
  }
}
