package org.dasein.cloud.azurearm;

public class AzureArmPaths {

    public enum AzureArmPath{
        LOCATION_RESOURCES("location", "/providers/Microsoft.Resources");

        private String uri;
        private String resource;
        private AzureArmPath(String resource, String uri){
            this.resource = resource;
            this.uri = uri;
        }

        public static String getUri(String resource){
            for(AzureArmPath path : AzureArmPath.values()){
                if(resource.equals(path.resource)){
                    return path.uri;
                }
            }
            throw new InternalError("Invalid resource requested - no path found.");
        }
    }
}
