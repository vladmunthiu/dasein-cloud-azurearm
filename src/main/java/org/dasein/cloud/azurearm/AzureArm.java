/*
 * Copyright (C) 2013-2014 Dell, Inc
 *
 * ====================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

package org.dasein.cloud.azurearm;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.dasein.cloud.AbstractCloud;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.ContextRequirements;
import org.dasein.cloud.dc.DataCenterServices;

import javax.annotation.Nonnull;

/**
 * Core cloud provider implementation for the Microsoft Azure cloud.
 * @author Drew Lyall (drew.lyall@imaginary.com)
 * @since 2015.06.1
 * @version 2015.06.1
 */
public class AzureArm extends AbstractCloud {
    private String apiVersion = "2015-01-01";

    public AzureArm(){}

    public HttpClientBuilder getAzureArmClientBuilder() throws CloudException {
        try {
            HttpClientBuilder builder = HttpClientBuilder.create();
            HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager();
            builder.setConnectionManager(ccm);
            return builder;
        } catch (Exception e) {
            throw new CloudException(e.getMessage());
        }
    }

    @Nonnull
    @Override
    public String getCloudName() {
        return "Azure";
    }

    @Override
    public @Nonnull
    ContextRequirements getContextRequirements() {
        return new ContextRequirements(
                new ContextRequirements.Field("apiSharedKey", "AD Application Client ID", ContextRequirements.FieldType.TEXT, true),
                new ContextRequirements.Field("apiSecretKey", "AD Tenant ID", ContextRequirements.FieldType.TEXT, true),
                new ContextRequirements.Field("proxyHost", "Proxy host", ContextRequirements.FieldType.TEXT, false),
                new ContextRequirements.Field("proxyPort", "Proxy port", ContextRequirements.FieldType.TEXT, false)
        );
    }

    @Nonnull
    @Override
    public DataCenterServices getDataCenterServices() {
        return new AzureArmLocation(this);
    }

    @Nonnull
    @Override
    public String getProviderName() {
        return "Microsoft";
    }


    public String getAPIVersion(){
        return apiVersion;
    }
}
