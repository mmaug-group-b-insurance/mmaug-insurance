package com.mmaug.claimsagent.document;

import com.azure.storage.queue.QueueClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;
import java.time.Duration;

@Component
@ConditionalOnProperty(name="app.scanner.provider",havingValue="defender-queue")
public class DefenderQueueConsumer {
    private final QueueClient queue;private final DefenderScanResults results;
    public DefenderQueueConsumer(QueueClient queue,DefenderScanResults results){this.queue=queue;this.results=results;}
    @Scheduled(fixedDelayString="${app.scanner.poll-ms:30000}",initialDelayString="${app.scanner.poll-ms:30000}")
    public void poll(){
        try{
            for(var message:queue.receiveMessages(10,Duration.ofSeconds(120),Duration.ofSeconds(15),com.azure.core.util.Context.NONE)){
                if(results.consume(message.getBody().toString()))queue.deleteMessage(message.getMessageId(),message.getPopReceipt());
                else LoggerFactory.getLogger(getClass()).warn("Defender result deferred; receive count {}",message.getDequeueCount());
            }
        }catch(RuntimeException e){LoggerFactory.getLogger(getClass()).warn("Defender queue unavailable ({})",e.getClass().getSimpleName());}
    }
}
