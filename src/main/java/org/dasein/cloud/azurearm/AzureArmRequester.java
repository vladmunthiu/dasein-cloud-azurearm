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
import org.dasein.cloud.azurearm.compute.vm.model.ArmVirtualMachineModel;
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
    private static final String LIST_VM_RESOURCES = "%s/subscriptions/%s/providers/Microsoft.Compute/virtualMachines?api-version=2015-06-15";
    private static final String CREATE_VM = "%s/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Compute/virtualMachines/%s?api-version=2015-06-15";
    private static final String STOP_VM = "%s/stop?api-version=2015-06-15";
    private static final String LIST_LOCATIONS = "%s/subscriptions/%s/providers/Microsoft.Compute?api-version=2015-01-01";
    private static final String AUTH_TOKEN_ENPOINT = "https://login.windows.net/%s/oauth2/token";
    private static final String RESOURCE_ENDPOINT = "https://management.core.windows.net/";

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

        protected RequestBuilder setRequestEntity(DaseinEntity entity) {
            return requestBuilder.setEntity(entity);
        }

        protected AzureArmRequester createRequester() throws CloudException {
            return new AzureArmRequester(provider, requestBuilder.build());
        }
    }

    public static class GetRequestUrlBuilder extends RequestUrlBuilder {
        private GetRequestUrlBuilder(RequestBuilder requestBuilder, AzureArm provider) {
            super(requestBuilder, provider);
        }

        public AzureArmRequester forLocations() throws CloudException {
            setRequestUri(LIST_LOCATIONS);
            return createRequester();
        }

        public AzureArmRequester forVirtualMachines() throws CloudException {
            setRequestUri(LIST_VM_RESOURCES);
            return createRequester();
        }
    }

    public static class PostRequestUrlBuilder extends RequestUrlBuilder{
        private PostRequestUrlBuilder(RequestBuilder requestBuilder, AzureArm provider) {
            super(requestBuilder, provider);
        }

        public AzureArmRequester forStopVirtualMachine(String id) throws CloudException {
            setRequestUri(String.format(STOP_VM, id));
            return createRequester();
        }
    }

    public static class PutRequestUrlBuilder extends RequestUrlBuilder {
        private PutRequestUrlBuilder(RequestBuilder requestBuilder, AzureArm provider) {
            super(requestBuilder, provider);
        }

        public AzureArmRequester forVirtualMachine(ArmVirtualMachineModel armVirtualMachineModel) throws CloudException, InternalException {
            String uri = getEncodedUri(String.format(CREATE_VM, provider.getContext().getCloud().getEndpoint(), provider.getContext().getAccountNumber(), armVirtualMachineModel.getResourceGroupName(), armVirtualMachineModel.getName()));
            setRequestUri(uri);
            setRequestEntity(new DaseinObjectToJsonEntity<ArmVirtualMachineModel>(armVirtualMachineModel));
            return createRequester();
        }
    }

    public static class DeleteRequestUrlBuilder extends RequestUrlBuilder {
        private DeleteRequestUrlBuilder(RequestBuilder requestBuilder, AzureArm provider){
            super(requestBuilder, provider);
        }
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

    public static PutRequestUrlBuilder createPutRequest(AzureArm provider) throws InternalException, CloudException {
        RequestBuilder requestBuilder = RequestBuilder.put();
        addCommonHeaders(provider, requestBuilder);
        return new PutRequestUrlBuilder(requestBuilder, provider);
    }

    public static DeleteRequestUrlBuilder createDeleteRequest(AzureArm provider) throws InternalException, CloudException {
        RequestBuilder requestBuilder = RequestBuilder.delete();
        addCommonHeaders(provider, requestBuilder);
        return new DeleteRequestUrlBuilder(requestBuilder, provider);
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
            String username = (String)provider.getContext().getConfigurationValue("username");
            String password = (String)provider.getContext().getConfigurationValue("password");
            String adTenantId = (String)provider.getContext().getConfigurationValue("adTenantId");
            String applicationId = (String)provider.getContext().getConfigurationValue("applicationId");

            AuthenticationContext context = new AuthenticationContext(String.format(AUTH_TOKEN_ENPOINT, adTenantId), true, service);
            Future<AuthenticationResult> future = context.acquireToken(RESOURCE_ENDPOINT, applicationId, username, password, null);
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
