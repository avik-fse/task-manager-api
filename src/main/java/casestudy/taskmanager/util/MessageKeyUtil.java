package casestudy.taskmanager.util;

import java.util.Arrays;
import java.util.List;

public class MessageKeyUtil {
  public static List<String> getAppKeys() {
    return Arrays.asList(
        "exception.taskExist",
        "exception.addTask",
        "exception.saveTask",
        "exception.saveParentTask",
        "exception.addParent",
        "exception.updateTaskEnded",
        "exception.taskNotFoundById",
        "exception.taskNotFound",
        "exception.parentTaskNotFoundById",
        "exception.parentTaskNotFound",
        "exception.priorityValidation",
        "error.invalidRequest",
        "error.taskNotFound",
        "error.invalidTaskId",
        "error.ignoreUpdate",
        "success.addTask",
        "success.updateTask",
        "label.task",
        "label.priority",
        "label.parentTask",
        "label.startDate",
        "label.endDate",
        "label.addTask",
        "label.reset",
        "label.cancel",
        "label.priorityFrom",
        "label.priorityTo",
        "label.start",
        "label.end",
        "label.parent",
        "label.edit",
        "label.endTask",
        "label.noTaskAvailable",
        "label.updateTask",
        "label.viewTask",
        "label.title");
  }
}
