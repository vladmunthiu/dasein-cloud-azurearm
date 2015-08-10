package org.dasein.cloud.azurearm;

import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.CloudProvider;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.util.requester.entities.DaseinEntity;
import org.dasein.cloud.util.requester.entities.DaseinObjectToJsonEntity;
import org.dasein.cloud.util.requester.fluent.DaseinRequest;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Provides simpler constructor for the underlying Dasein request object
 * @author Drew Lyall (drew.lyall@imaginary.com)
 * @since 2015.06.1
 * @version 2015.06.1
 */
public class AzureArmRequester extends DaseinRequest{
    private static final String LIST_VM_RESOURCES = "%s/subscriptions/%s/providers/Microsoft.Compute/virtualMachine?api-version=2015-06-01";
    private static final String LIST_LOCATIONS = "%s/subscriptions/%s/providers/Microsoft.Compute?api-version=2015-01-01";

    public AzureArmRequester(AzureArm provider, HttpUriRequest httpUriRequest) throws CloudException {
        this(provider, provider.getAzureArmClientBuilder(), httpUriRequest);
    }
    public AzureArmRequester(CloudProvider provider, HttpClientBuilder httpClientBuilder, HttpUriRequest httpUriRequestBuilder) {
        super(provider, httpClientBuilder, httpUriRequestBuilder);
    }

    public static abstract class RequestUrlBuilder {
        protected RequestBuilder requestBuilder;
        protected AzureArm provider;
        private RequestUrlBuilder(RequestBuilder requestBuilder, AzureArm provider) {
            this.requestBuilder = requestBuilder;
            this.provider = provider;
        }

        protected RequestBuilder setRequestUri(String uri) {
            return requestBuilder.setUri(String.format(uri, provider.getContext().getCloud().getEndpoint(), provider.getContext().getAccountNumber()));
        }

        protected RequestBuilder setEntity(DaseinEntity entity) {
            return requestBuilder.setEntity(entity);
        }
    }

    public static class GetRequestUrlBuilder extends RequestUrlBuilder {
        private GetRequestUrlBuilder(RequestBuilder requestBuilder, AzureArm provider) {
            super(requestBuilder, provider);
        }

        public RequestBuilder locations() {
            return this.setRequestUri(LIST_LOCATIONS);
        }
        public RequestBuilder virtualMachines(){
            return this.setRequestUri(LIST_VM_RESOURCES);
        }
    }

    public static class PostRequestUrlBuilder extends RequestUrlBuilder{
        private PostRequestUrlBuilder(RequestBuilder requestBuilder, AzureArm provider) {
            super(requestBuilder, provider);
        }

//        public RequestBuilder virtualMachines(WAPVirtualMachineModel virtualMachineModel) {
//            return setUri(LIST_VM_RESOURCES).setEntity(new DaseinObjectToJsonEntity<WAPVirtualMachineModel>(virtualMachineModel));
//        }
    }

    public static GetRequestUrlBuilder createGetRequest(AzureArm provider) throws CloudException {
        RequestBuilder requestBuilder = RequestBuilder.get();
        addCommonHeaders(provider, requestBuilder);
        return new GetRequestUrlBuilder(requestBuilder, provider);
    }

    public static PostRequestUrlBuilder createPostRequest(AzureArm provider) throws CloudException {
        RequestBuilder requestBuilder = RequestBuilder.post();
        addCommonHeaders(provider, requestBuilder);
        return new PostRequestUrlBuilder(requestBuilder, provider);
    }

    public static RequestBuilder createPutRequest(String uri, AzureArm provider, DaseinEntity entity, String...uriParts) throws InternalException, CloudException {
        RequestBuilder requestBuilder = RequestBuilder.put();
        addCommonHeaders(provider, requestBuilder);
        requestBuilder.setUri(getEncodedUri(String.format(uri, provider.getContext().getCloud().getEndpoint(), provider.getContext().getAccountNumber(), uriParts)));
        requestBuilder.setEntity(entity);
        return requestBuilder;
    }

    public static RequestBuilder createDeleteRequest(AzureArm provider, String uri, String...uriParts) throws InternalException, CloudException {
        RequestBuilder requestBuilder = RequestBuilder.delete();
        addCommonHeaders(provider, requestBuilder);
        requestBuilder.setUri(getEncodedUri(String.format(uri, provider.getContext().getCloud().getEndpoint(), provider.getContext().getAccountNumber(), uriParts)));
        return requestBuilder;
    }

    private static String getEncodedUri(String urlString) throws InternalException {
        try {
            URL url = new URL(urlString);
            return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef()).toString();
        } catch (Exception e) {
            throw new InternalException(e.getMessage());
        }
    }

    private static void addCommonHeaders(AzureArm provider, RequestBuilder requestBuilder) throws CloudException {
        requestBuilder.addHeader("Content-Type", "application/json");
        String authenticationToken = getAuthenticationToken(provider);
        requestBuilder.addHeader("Authorization", String.format("Bearer %s", authenticationToken));
    }

    private static String getAuthenticationToken(AzureArm provider) throws CloudException{
        ExecutorService service = Executors.newFixedThreadPool(1);;
        try {
            String username = "";
            String password = "";
            String adTenantId = "";
            String applicationId = "";

            List<ContextRequirements.Field> fields = provider.getContextRequirements().getConfigurableValues();
            for(ContextRequirements.Field f : fields ) {
                if(f.name.equals("username"))username = (String)provider.getContext().getConfigurationValue(f);
                else if(f.name.equals("password"))password = (String)provider.getContext().getConfigurationValue(f);
                else if(f.name.equals("adTenantId"))adTenantId = (String)provider.getContext().getConfigurationValue(f);
                else if(f.name.equals("applicationId"))applicationId = (String)provider.getContext().getConfigurationValue(f);
            }
            AuthenticationContext context = new AuthenticationContext("https://login.windows.net/" + adTenantId + "/oauth2/token", true, service);
            Future<AuthenticationResult> future = context.acquireToken("https://management.core.windows.net/", applicationId, username, password, null);
            AuthenticationResult result = future.get();
            return result.getAccessToken();
        } catch(Exception ex){
            throw new CloudException("Could not obtain authentication token. " + ex.getMessage());
        }
        finally {
            service.shutdown();
        }
    }
}
