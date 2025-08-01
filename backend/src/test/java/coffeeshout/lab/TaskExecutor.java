package coffeeshout.lab;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TaskExecutor {
    private static final Logger logger = LoggerFactory.getLogger(TaskExecutor.class);

    // 대충 Spring이 TaskScheduler 를 처리할 수 있도록 함
    private TaskScheduler scheduler;
    private final Queue<Task> taskQueue = new ConcurrentLinkedQueue<>();
    private final Map<Long, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private volatile boolean isRunning = false;

    public TaskExecutor() {
        // 빈 생성자
    }

    public TaskExecutor(Task... tasks) {
        for (Task task : tasks) {
            addTask(task);
        }
    }

    public synchronized void addTask(Task task) {
        taskQueue.offer(task);
        logger.debug("Task {} added to queue", task.getId());
        if (!isRunning) {
            processNextTask();
        }
    }

    public synchronized void start() {
        if (!isRunning) {
            isRunning = true;
            processNextTask();
        }
    }

    // 아마 취소를 하게 된다면 현재 큐에 있는 것을 취소하지 않을까 
    public synchronized void cancel() {
        // 실행 중인 작업 취소
        ScheduledFuture<?> future = futures.get(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            futures.remove(taskId);
            logger.debug("Task {} cancelled", taskId);
        }
        
        // 큐에서 대기 중인 작업 제거
        taskQueue.removeIf(task -> task.getId().equals(taskId));
        
        // 다음 작업 처리
        processNextTask();
    }

    private void processNextTask() {
        Task nextTask = taskQueue.poll();
        if (nextTask != null) {
            logger.debug("Processing task: {}", nextTask.getId());
            ScheduledFuture<?> future = nextTask.execute(scheduler, () -> {
                futures.remove(nextTask.getId());
                cleanupFinishedFutures();
                processNextTask(); // 완료 후 다음 작업 처리
            });
            futures.put(nextTask.getId(), future);
        } else {
            isRunning = false;
            logger.debug("No more tasks to process");
        }
    }

    public synchronized void shutdown() {
        isRunning = false;
        taskQueue.clear();
        futures.values().forEach(future -> future.cancel(false));
        futures.clear();
        if (scheduler instanceof ThreadPoolTaskScheduler) {
            ((ThreadPoolTaskScheduler) scheduler).shutdown();
        }
        logger.info("TaskExecutor shutdown completed");
    }

    public void cleanupFinishedFutures() {
        futures.entrySet().removeIf(entry -> entry.getValue().isDone());
    }

    public int getQueueSize() {
        return taskQueue.size();
    }

    public int getActiveTaskCount() {
        return (int) futures.values().stream().filter(f -> !f.isDone()).count();
    }
}
