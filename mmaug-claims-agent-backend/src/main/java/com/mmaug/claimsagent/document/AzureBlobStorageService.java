package com.mmaug.claimsagent.document;

import com.azure.core.util.Context;
import com.azure.storage.blob.*;
import com.azure.storage.blob.models.*;
import com.azure.storage.blob.options.BlobInputStreamOptions;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Set;

@Service
@ConditionalOnProperty(name="app.storage.provider",havingValue="azure")
public class AzureBlobStorageService implements BlobStorageService {
    private final BlobServiceClient service;
    private static final Set<String> CONTAINERS=Set.of("quarantine", "clean");
    public AzureBlobStorageService(BlobServiceClient service){this.service=service;}

    static void requireEndpoint(String value,String suffix){
        URI uri=URI.create(value);
        if(!"https".equals(uri.getScheme()) || uri.getHost()==null || !uri.getHost().matches("[a-z0-9]{3,24}"+java.util.regex.Pattern.quote(suffix))
                || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null || uri.getPort()!=-1
                || (suffix.startsWith(".blob") && !Set.of("", "/").contains(uri.getPath()))
                || (suffix.startsWith(".queue") && !uri.getPath().matches("/[a-z0-9][a-z0-9-]{1,61}[a-z0-9]")))
            throw new IllegalArgumentException("Azure evidence requires a configured HTTPS storage endpoint without credentials");
    }

    private BlobClient blob(String container,String name){
        if(!CONTAINERS.contains(container) || name==null || name.isBlank() || name.startsWith("/")
                || java.util.Arrays.stream(name.split("/")).anyMatch(Set.of(".","..")::contains) || name.contains("\\") || name.contains("?") || name.contains("#"))
            throw new IllegalArgumentException("Invalid evidence storage address");
        var client=service.getBlobContainerClient(container);
        // Refuse publicly readable containers. Provisioning is separate; never create containers here.
        if(client.getProperties().getBlobPublicAccess()!=null)throw new IllegalStateException("Evidence container must be private");
        return client.getBlobClient(name);
    }

    @Override public void store(String container,String name,InputStream content,long size,String type){
        if(size<0 || size>15L*1024*1024)throw new IllegalArgumentException("Evidence exceeds storage limit");
        try{
            blob(container,name).getBlockBlobClient().uploadWithResponse(content,size,
                    new BlobHttpHeaders().setContentType(type),null,null,null,
                    new BlobRequestConditions().setIfNoneMatch("*"),Duration.ofSeconds(30),Context.NONE);
        }catch(RuntimeException e){throw new IllegalStateException("Private evidence write failed");}
    }
    @Override public String version(String container,String name){
        try{return blob(container,name).getProperties().getETag();}
        catch(RuntimeException e){throw new IllegalStateException("Private evidence version lookup failed");}
    }
    @Override public InputStream retrieve(String container,String name){return retrieveVersion(container,name,version(container,name));}
    @Override public InputStream retrieveVersion(String container,String name,String etag){
        if(etag==null || etag.isBlank())throw new IllegalArgumentException("Evidence version required");
        try{return blob(container,name).openInputStream(new BlobInputStreamOptions().setRequestConditions(new BlobRequestConditions().setIfMatch(etag)));}
        catch(RuntimeException e){throw new IllegalStateException("Private evidence read failed");}
    }
    @Override public void delete(String container,String name){
        try{blob(container,name).deleteIfExists();}
        catch(RuntimeException e){throw new IllegalStateException("Private evidence delete failed");}
    }
    @Override public String generateDownloadUrl(String container,String name,int expiry){
        throw new UnsupportedOperationException("Evidence downloads require the authorised backend endpoint");
    }
}
