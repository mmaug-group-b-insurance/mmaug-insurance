package com.mmaug.claimsagent.common;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
@Component
public class DatabaseWork {
 private final TransactionTemplate write;
 public DatabaseWork(PlatformTransactionManager manager){write=new TransactionTemplate(manager);write.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);}
 public <T>T write(Supplier<T> action){return write.execute(status->action.get());}
}
