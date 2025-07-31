package coffeeshout.lab;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Task {
    private static final Logger logger = LoggerFactory.getLogger(Task.class);
    private final Long id;
    private final Runnable runnable;
    private final Long duration;

    public Task(Long id, Runnable runnable, Long duration) {
        this.id = id;
        this.runnable = runnable;
        this.duration = duration;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof final Task task)) {
            return false;
        }
        return Objects.equals(id, task.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public Long getId() {
        return this.id;
    }

    public ScheduledFuture<?> execute(TaskScheduler scheduler, Runnable onComplete) {
        Runnable taskRunnable = () -> {
            try {
                logger.debug("Executing task: {}", id);
                runnable.run();
            } catch (Exception e) {
                logger.error("Task {} execution failed: {}", id, e.getMessage(), e);
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        };
        return scheduler.schedule(
                taskRunnable,
                new Date(System.currentTimeMillis() + duration).toInstant()
        );
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public Long getDuration() {
        return duration;
    }
}