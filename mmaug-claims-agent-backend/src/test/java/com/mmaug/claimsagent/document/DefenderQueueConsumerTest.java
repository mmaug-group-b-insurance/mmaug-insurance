package com.mmaug.claimsagent.document;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.models.QueueMessageItem;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class DefenderQueueConsumerTest {
    private final QueueClient queue=mock(QueueClient.class);
    private final DefenderScanResults results=mock(DefenderScanResults.class);
    private final DefenderQueueConsumer consumer=new DefenderQueueConsumer(queue,results);
    @SuppressWarnings("unchecked") DefenderQueueConsumerTest(){
        var item=mock(QueueMessageItem.class);when(item.getBody()).thenReturn(BinaryData.fromString("synthetic event"));
        when(item.getMessageId()).thenReturn("message");when(item.getPopReceipt()).thenReturn("receipt");
        PagedIterable<QueueMessageItem> batch=mock(PagedIterable.class);when(batch.iterator()).thenAnswer(i->List.of(item).iterator());
        when(queue.receiveMessages(eq(10),any(),any(),any())).thenReturn(batch);
    }
    @Test void acknowledgesOnlyAfterCommittedResult(){
        when(results.consume("synthetic event")).thenReturn(true);consumer.poll();
        var order=inOrder(results,queue);order.verify(results).consume("synthetic event");order.verify(queue).deleteMessage("message","receipt");
    }
    @Test void deferredResultIsNotDeleted(){
        when(results.consume("synthetic event")).thenReturn(false);consumer.poll();verify(queue,never()).deleteMessage(anyString(),anyString());
    }
    @Test void databaseFailureRetainsMessageAndDoesNotLeakException(){
        when(results.consume("synthetic event")).thenThrow(new IllegalStateException("private diagnostic"));
        assertThatCode(consumer::poll).doesNotThrowAnyException();verify(queue,never()).deleteMessage(anyString(),anyString());
    }
}
