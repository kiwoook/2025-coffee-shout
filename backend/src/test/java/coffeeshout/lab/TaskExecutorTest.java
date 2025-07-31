package coffeeshout.lab;

import org.junit.jupiter.api.Test;

class TaskExecutorTest {

    @Test
    void queueBasedTaskExecutionTest() throws InterruptedException {
        // given
        TaskExecutor executor = new TaskExecutor();
        
        // 여러 작업을 동적으로 추가
        executor.addTask(new Task(100L, () -> System.out.println("Priority task"), 500L));
        
        Thread.sleep(100);
        
        // 실행 중에 새 작업 추가
        executor.addTask(new Task(200L, () -> System.out.println("Added during execution"), 0L));
        
        Thread.sleep(2000);
        
        executor.shutdown();
    }

    @Test
    void TaskExecutorTest() throws InterruptedException {
        // given
        Task task1 = new Task(1L, () -> System.out.println("Task 1: Immediate"), 0L);
        Task task2 = new Task(2L, () -> System.out.println("Task 2: After 1 second"), 1000L);
        Task task3 = new Task(3L, () -> System.out.println("Task 3: After 2 seconds"), 2000L);
        Task task4 = new Task(4L, () -> System.out.println("Task 4: Immediate"), 0L);

        TaskExecutor taskExecutor = new TaskExecutor();

        // when - 작업들을 큐에 추가
        taskExecutor.addTask(task1);
        taskExecutor.addTask(task2);
        taskExecutor.addTask(task3);
        taskExecutor.addTask(task4);
        
        System.out.println("Queue size: " + taskExecutor.getQueueSize());
        
        // 실행 시작
        taskExecutor.start();
        
        Thread.sleep(1500); // 1.5초 대기
        
        // task2 취소 테스트
        System.out.println("Cancelling task 2...");
        taskExecutor.cancelTask(2L);
        
        Thread.sleep(3000); // 3초 더 대기
        
        System.out.println("Final queue size: " + taskExecutor.getQueueSize());
        System.out.println("Active tasks: " + taskExecutor.getActiveTaskCount());
        
        taskExecutor.shutdown();
        
        // then
        // 콘솔 출력으로 검증
    }}
