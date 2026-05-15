package ch.uzh.ifi.hase.soprafs26.entity;

import java.time.LocalDateTime;
import java.time.Duration;

public class GameTimer {
    private LocalDateTime startTime;
    private final long timeLimitSeconds;
    private boolean paused;
    private long remainingSecondsWhenPaused;
    
    public GameTimer(long timeLimitSeconds) {
        this.startTime = LocalDateTime.now();
        this.timeLimitSeconds = timeLimitSeconds;
    }
    
    public long getRemainingSeconds(LocalDateTime now) {
        if (paused) {
            return remainingSecondsWhenPaused;
        }
        long elapsed = Duration.between(startTime, now).getSeconds();
        return Math.max(0, timeLimitSeconds - elapsed);
    }

    public void pause(LocalDateTime now) {
        if (!paused) {
            remainingSecondsWhenPaused = getRemainingSeconds(now);
            paused = true;
        }
    }

    public void resume(LocalDateTime now) {
        if (paused) {
            startTime = now.minusSeconds(timeLimitSeconds - remainingSecondsWhenPaused);
            paused = false;
        }
    }

    public boolean isPaused() {
        return paused;
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public long getTimeLimitSeconds() {
        return timeLimitSeconds;
    }
} 
