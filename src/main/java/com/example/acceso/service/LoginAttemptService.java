package com.example.acceso.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private final int MAX_ATTEMPTS = 3;
    private final int BLOCK_DURATION_MINUTES = 2;

    private ConcurrentHashMap<String, Integer> attemptsCache = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, LocalDateTime> blockCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String key) {
        attemptsCache.remove(key);
        blockCache.remove(key);
    }

    public void loginFailed(String key) {
        int attempts = attemptsCache.getOrDefault(key, 0);
        attempts++;
        attemptsCache.put(key, attempts);

        if (attempts >= MAX_ATTEMPTS) {
            blockCache.put(key, LocalDateTime.now());
        }
    }

    public boolean isBlocked(String key) {
        if (blockCache.containsKey(key)) {
            LocalDateTime blockTime = blockCache.get(key);
            if (ChronoUnit.MINUTES.between(blockTime, LocalDateTime.now()) >= BLOCK_DURATION_MINUTES) {
                // Bloqueo expirado
                attemptsCache.remove(key);
                blockCache.remove(key);
                return false;
            }
            return true;
        }
        return false;
    }

    public long getRemainingBlockTime(String key) {
         if (blockCache.containsKey(key)) {
            LocalDateTime blockTime = blockCache.get(key);
            long secondsPassed = ChronoUnit.SECONDS.between(blockTime, LocalDateTime.now());
            long totalBlockSeconds = BLOCK_DURATION_MINUTES * 60;
            return totalBlockSeconds - secondsPassed;
         }
         return 0;
    }
}
