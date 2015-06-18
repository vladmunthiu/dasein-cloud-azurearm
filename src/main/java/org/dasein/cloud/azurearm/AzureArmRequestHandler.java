package org.dasein.cloud.azurearm;

import org.apache.http.client.methods.RequestBuilder;
import org.dasein.cloud.ContextRequirements;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

/**
 * Handles the various requests going to Azure Arm
 * @author Drew Lyall (drew.lyall@imaginary.com)
 * @since 2015.06.1
 * @version 2015.06.1
 */
public class AzureArmRequestHandler {
    private AzureArm provider;

    public AzureArmRequestHandler(AzureArm provider){
        this.provider = provider;
    }

    public RequestBuilder get(@Nonnull String resource, @Nullable Map<String, String> parameters){
        RequestBuilder requestBuilder = RequestBuilder.get();
        addCommonHeaders(requestBuilder);
        addAuth(requestBuilder);

        String uri = provider.getContext().getCloud().getEndpoint() + AzureArmPaths.AzureArmPath.getUri(resource) + "?";
        if(parameters != null && !parameters.isEmpty()){
            Iterator<String> it = parameters.keySet().iterator();
            while(it.hasNext()){
                String key = it.next();
                uri += key + "=" + parameters.get(key);
                uri += "&";
            }
        }
        uri += "api-version=" + provider.getAPIVersion();
        requestBuilder.setUri(uri);
        return requestBuilder;
    }

    private void addCommonHeaders(RequestBuilder requestBuilder) {
        requestBuilder.addHeader("Content-Type", "application/json");
    }

    private void addAuth(RequestBuilder requestBuilder){
        ContextRequirements contextRequirements = provider.getContextRequirements();

        for(ContextRequirements.Field field : contextRequirements.getConfigurableValues()){
            field.name
        }

        String uri = "https://login.windows.net/%s/oauth2/authorize?client_id=%s&response_type=code";
        RequestBuilder authRequest = RequestBuilder.get();
        requestBuilder.setUri(String.format(uri), )
    }
}
