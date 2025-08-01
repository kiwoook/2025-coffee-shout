package coffeeshout.lab;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class LabGameConfig {
    
    @Bean(name = "labGameTaskScheduler")
    public TaskScheduler labGameTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setCorePoolSize(10);
        scheduler.setMaxPoolSize(30);
        scheduler.setQueueCapacity(100);
        scheduler.setThreadNamePrefix("lab-game-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}