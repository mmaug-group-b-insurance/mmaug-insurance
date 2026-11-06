package com.mmaug.claimsagent.document;

import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.specialized.BlockBlobClient;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AzureBlobStorageServiceTest {
    private final BlobServiceClient service=mock(BlobServiceClient.class);
    private final BlobContainerClient container=mock(BlobContainerClient.class);
    private final BlobClient blob=mock(BlobClient.class);
    private final BlockBlobClient block=mock(BlockBlobClient.class);
    private final BlobContainerProperties properties=mock(BlobContainerProperties.class);
    private final AzureBlobStorageService storage=new AzureBlobStorageService(service);
    AzureBlobStorageServiceTest(){when(service.getBlobContainerClient("quarantine")).thenReturn(container);when(container.getProperties()).thenReturn(properties);when(container.getBlobClient("synthetic.png")).thenReturn(blob);when(blob.getBlockBlobClient()).thenReturn(block);}
    @Test void writesWithoutOverwriteOrContainerCreation(){
        storage.store("quarantine","synthetic.png",new ByteArrayInputStream(new byte[]{1}),1,"image/png");
        verify(block).uploadWithResponse(any(),eq(1L),argThat(h->h.getContentType().equals("image/png")),isNull(),isNull(),isNull(),argThat(c->"*".equals(c.getIfNoneMatch())),any(),any());
        verify(container,never()).create();
    }
    @Test void publicContainersFailClosed(){
        when(properties.getBlobPublicAccess()).thenReturn(PublicAccessType.BLOB);
        assertThatThrownBy(()->storage.version("quarantine","synthetic.png")).hasMessage("Private evidence version lookup failed");verifyNoInteractions(blob);
    }
    @Test void invalidAddressesAndSasRequestsNeverTouchAzure(){
        assertThatThrownBy(()->storage.version("other","synthetic.png")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->storage.version("quarantine","../secret")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->storage.generateDownloadUrl("quarantine","synthetic.png",5)).isInstanceOf(UnsupportedOperationException.class);verifyNoInteractions(service);
    }
    @Test void requiresVersionForRead(){assertThatThrownBy(()->storage.retrieveVersion("quarantine","synthetic.png",null)).isInstanceOf(IllegalArgumentException.class);verifyNoInteractions(service);}
    @Test void externalReadIsPinnedWithIfMatch(){
        storage.retrieveVersion("quarantine","synthetic.png","version-one");
        verify(blob).openInputStream(argThat((com.azure.storage.blob.options.BlobInputStreamOptions options)->"version-one".equals(options.getRequestConditions().getIfMatch())));
    }
    @Test void storageErrorsDoNotExposeProviderText(){
        when(container.getProperties()).thenThrow(new IllegalStateException("private account diagnostic"));
        assertThatThrownBy(()->storage.version("quarantine","synthetic.png")).hasMessage("Private evidence version lookup failed").hasNoCause();
    }
    @Test void rejectsEndpointsWithCredentialsPathsAndWrongHost(){
        AzureBlobStorageService.requireEndpoint("https://syntheticstore.blob.core.windows.net/",".blob.core.windows.net");
        for(String endpoint:new String[]{"http://syntheticstore.blob.core.windows.net","https://user@syntheticstore.blob.core.windows.net","https://syntheticstore.blob.core.windows.net?sig=secret","https://syntheticstore.blob.core.windows.net/path","https://example.invalid"})
            assertThatThrownBy(()->AzureBlobStorageService.requireEndpoint(endpoint,".blob.core.windows.net")).isInstanceOf(IllegalArgumentException.class);
    }
}
