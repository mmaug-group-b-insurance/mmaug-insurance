package com.mmaug.claimsagent.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

@Component
public class AgentRunRecovery {
    private static final Logger log = LoggerFactory.getLogger(AgentRunRecovery.class);
    private final AgentRunLifecycle lifecycle;

    public AgentRunRecovery(AgentRunLifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void recover() {
        try {
            int count = lifecycle.recoverExpired();
            if (count > 0) log.warn("Marked {} expired analysis reservations failed", count);
        } catch (RuntimeException e) {
            log.warn("Analysis recovery unavailable ({})", e.getClass().getSimpleName());
        }
    }
}
