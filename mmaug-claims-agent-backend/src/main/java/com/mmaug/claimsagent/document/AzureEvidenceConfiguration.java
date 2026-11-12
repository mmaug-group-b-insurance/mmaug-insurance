package com.mmaug.claimsagent.document;

import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.storage.blob.*;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.queue.*;
import com.azure.storage.queue.QueueMessageEncoding;
import com.azure.storage.common.policy.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name="app.storage.provider", havingValue="azure")
public class AzureEvidenceConfiguration {
    @Bean BlobServiceClient evidenceBlobClient(@Value("${app.storage.endpoint}") String endpoint,
                                               @Value("${app.storage.account-key:}") String accountKey,
                                               @Value("${app.storage.managed-identity-client-id:}") String clientId) {
        AzureBlobStorageService.requireEndpoint(endpoint, ".blob.core.windows.net");
        var builder = new BlobServiceClientBuilder().endpoint(endpoint);
        if (accountKey != null && !accountKey.isBlank()) {
            String host = java.net.URI.create(endpoint).getHost();
            String accountName = host.split("\\.")[0];
            builder.credential(new StorageSharedKeyCredential(accountName, accountKey));
        } else {
            var identity = new ManagedIdentityCredentialBuilder();
            if(!clientId.isBlank()) identity.clientId(clientId);
            builder.credential(identity.build());
        }
        return builder.retryOptions(new RequestRetryOptions(RetryPolicyType.FIXED, 2, 10, 500L, 1000L, null)).buildClient();
    }

    @Bean
    @ConditionalOnProperty(name="app.scanner.provider", havingValue="defender-queue")
    QueueClient defenderResultQueue(@Value("${app.scanner.queue-endpoint}") String endpoint,
                                    @Value("${app.storage.account-key:}") String accountKey,
                                    @Value("${app.storage.managed-identity-client-id:}") String clientId) {
        AzureBlobStorageService.requireEndpoint(endpoint, ".queue.core.windows.net");
        var builder = new QueueClientBuilder().endpoint(endpoint).messageEncoding(QueueMessageEncoding.BASE64);
        if (accountKey != null && !accountKey.isBlank()) {
            String host = java.net.URI.create(endpoint).getHost();
            String accountName = host.split("\\.")[0];
            builder.credential(new StorageSharedKeyCredential(accountName, accountKey));
        } else {
            var identity = new ManagedIdentityCredentialBuilder();
            if(!clientId.isBlank()) identity.clientId(clientId);
            builder.credential(identity.build());
        }
        return builder.retryOptions(new RequestRetryOptions(RetryPolicyType.FIXED, 2, 10, 500L, 1000L, null)).buildClient();
    }
}
