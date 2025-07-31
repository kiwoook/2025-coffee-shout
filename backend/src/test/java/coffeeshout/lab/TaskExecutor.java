package coffeeshout.lab;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class TaskExecutor<T> {

    TaskScheduler scheduler = new ThreadPoolTaskScheduler() {{
        this.setPoolSize(1);
        this.setThreadNamePrefix("my-task-");
        this.initialize();
    }};
    private final Map<Long, ScheduledFuture<?>> futures = new HashMap<>();

    private Task currentTask;

    public TaskExecutor(Task firstTask) {
        this.currentTask = firstTask;
    }

    public void start() {
        ScheduledFuture<?> future = currentTask.start(this, futures);
        futures.put(currentTask.getId(), future);
    }

    // 현재 실행중인 future 취소 후 다음 작업이 현재 작업이 된다.
    public void cancelCurrentFuture(Task targetTask) {
        ScheduledFuture<?> targetFuture = futures.get(targetTask.getId());
        if (targetFuture == null) {
            throw new IllegalStateException("아직 시작되지 않은 작업입니다.");
        }
        targetFuture.cancel(false);
        currentTask = currentTask.getNextTask();
    }

    public void setCurrentTask(Task nextTask) {
        this.currentTask = nextTask;
    }
}
