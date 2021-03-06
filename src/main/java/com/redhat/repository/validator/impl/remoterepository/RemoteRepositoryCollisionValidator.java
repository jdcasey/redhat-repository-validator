package com.redhat.repository.validator.impl.remoterepository;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;

public class RemoteRepositoryCollisionValidator extends RemoteRepositoryAbstractValidator {
    
    private final ChecksumProvider checksumProvider;

    public RemoteRepositoryCollisionValidator(String remoteRepositoryUrl, ChecksumProvider checksumProvider) {
        this(remoteRepositoryUrl, checksumProvider, FileFilterUtils.trueFileFilter(), 20);
    }

    public RemoteRepositoryCollisionValidator(String remoteRepositoryUrl, ChecksumProvider checksumProvider, IOFileFilter fileFilter, int maxConnTotal) {
        super(remoteRepositoryUrl, fileFilter, maxConnTotal);
        this.checksumProvider = checksumProvider;
    }

    @Override
    protected void validateArtifact(CloseableHttpClient httpClient, URI localArtifact, URI remoteArtifact) throws Exception {
        try {
            HttpUriRequest httpRequest = RequestBuilder.head().setUri(remoteArtifact).build();
            HttpResponse httpResponse = httpClient.execute(httpRequest);
            int httpStatusCode = httpResponse.getStatusLine().getStatusCode();
            if (httpStatusCode == HttpStatus.SC_OK) {
                String remoteArtifactHash = checksumProvider.getRemoteArtifactChecksum(remoteArtifact, httpResponse);
                String localArtifactHash = checksumProvider.getLocalArtifactChecksum(localArtifact);

                if (!equalsIgnoreCase(remoteArtifactHash, localArtifactHash)) {
                    throw new RemoteRepositoryCollisionException("Remote repository [" + remoteRepositoryUrl + "] contains already artifact " + remoteArtifact + " with different content");
                }

            } else if (httpStatusCode == HttpStatus.SC_NOT_FOUND) {
                // OK
            } else {
                throw new RemoteRepositoryCollisionException("Remote repository [" + remoteRepositoryUrl + "] returned " + httpResponse.getStatusLine().toString() + " for artifact " + remoteArtifact);
            }
        } catch (IOException e) {
            throw new RemoteRepositoryCollisionException("Remote repository [" + remoteRepositoryUrl + "] request failed for artifact " + remoteArtifact, e);
        }
    }

}