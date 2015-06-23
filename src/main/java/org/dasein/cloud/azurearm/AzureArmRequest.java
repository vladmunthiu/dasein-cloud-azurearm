package org.dasein.cloud.azurearm;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.util.requester.fluent.DaseinRequest;

/**
 * Provides simpler constructor for the underlying Dasein request object
 * @author Drew Lyall (drew.lyall@imaginary.com)
 * @since 2015.06.1
 * @version 2015.06.1
 */
public class AzureArmRequest extends DaseinRequest{
    public AzureArmRequest(AzureArm provider, HttpUriRequest httpUriRequest) throws CloudException {
        this(provider, provider.getAzureArmClientBuilder(), httpUriRequest);
    }
    public AzureArmRequest(CloudProvider provider, HttpClientBuilder httpClientBuilder, HttpUriRequest httpUriRequestBuilder) {
        super(provider, httpClientBuilder, httpUriRequestBuilder);
    }
}
