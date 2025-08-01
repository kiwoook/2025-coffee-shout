package coffeeshout.game.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
public class GameTaskConfig {
    
    @Bean(name = "gameTaskScheduler")
    public TaskScheduler gameTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setCorePoolSize(20);
        scheduler.setMaxPoolSize(50);
        scheduler.setQueueCapacity(200);
        scheduler.setThreadNamePrefix("game-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}