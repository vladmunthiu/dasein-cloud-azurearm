package org.dasein.cloud.azurearm;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.aad.adal4j.ClientCredential;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dasein.cloud.ContextRequirements;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.ServiceUnavailableException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

    private void addAuth(RequestBuilder requestBuilder) {
        AuthenticationContext context = null;
        AuthenticationResult result = null;
        ExecutorService service = null;
        try {
            String username = "";
            String password = "";
            String adTenantId = "";

            List<ContextRequirements.Field> fields = provider.getContextRequirements().getConfigurableValues();
            for(ContextRequirements.Field f : fields ) {
                if(f.name.equals("username"))username = (String)provider.getContext().getConfigurationValue(f);
                else if(f.name.equals("password"))password = (String)provider.getContext().getConfigurationValue(f);
                else if(f.name.equals("adTenantId"))adTenantId = (String)provider.getContext().getConfigurationValue(f);
            }
            service = Executors.newFixedThreadPool(1);
            context = new AuthenticationContext("https://login.windows.net/" + adTenantId + "/oauth2/token", true, service);
            Future<AuthenticationResult> future = context.acquireToken("https://management.core.windows.net/", provider.getContext().getAccountNumber(), username, password, null);
            result = future.get();
            requestBuilder.addHeader("Authorization", result.getAccessToken());

        } catch(Exception ex){
            //TODO: Fix me properly
            ex.printStackTrace();
        }
        finally {
            service.shutdown();
        }
    }
}
