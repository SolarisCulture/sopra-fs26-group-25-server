package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;
import java.time.Duration;

public class GameTimer {
    private final LocalDateTime startTime;
    private final long timeLimitSeconds;
    
    public GameTimer(long timeLimitSeconds) {
        this.startTime = LocalDateTime.now();
        this.timeLimitSeconds = timeLimitSeconds;
    }
    
    public long getRemainingSeconds(LocalDateTime now) {
        long elapsed = Duration.between(startTime, now).getSeconds();
        return Math.max(0, timeLimitSeconds - elapsed);
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public long getTimeLimitSeconds() {
        return timeLimitSeconds;
    }
} 
