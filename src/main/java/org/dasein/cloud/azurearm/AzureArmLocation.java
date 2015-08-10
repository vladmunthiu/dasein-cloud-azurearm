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

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.http.client.methods.HttpUriRequest;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azurearm.model.ArmProviderModel;
import org.dasein.cloud.azurearm.model.ArmResourceTypeModel;
import org.dasein.cloud.dc.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Displays the available locations for Microsoft Azure services.
 * @author Drew Lyall (drew.lyall@imaginary.com)
 * @since 2015.06.1
 * @version 2015.06.1
 */
public class AzureArmLocation implements DataCenterServices{
    private AzureArm provider;

    AzureArmLocation(AzureArm provider) { this.provider = provider; }

    private transient volatile AzureArmLocationCapabilities capabilities;
    @Nonnull
    @Override
    public DataCenterCapabilities getCapabilities() throws InternalException, CloudException {
        if( capabilities == null ) {
            capabilities = new AzureArmLocationCapabilities(provider);
        }
        return capabilities;
    }

    @Override
    public String getProviderTermForDataCenter(Locale locale) {
        return "datacenter";
    }

    @Override
    public String getProviderTermForRegion(Locale locale) {
        return "location";
    }

    @Override
    public @Nonnull Region getRegion(@Nonnull String providerRegionId) throws InternalException, CloudException {
        for(Region region : listRegions()){
            if(providerRegionId.equals(region.getProviderRegionId()))return region;
        }
        throw new InternalException("The requested region does not exist or cannot be found");
    }

    @Nonnull
    @Override
    public Iterable<DataCenter> listDataCenters(@Nonnull String providerRegionId) throws InternalException, CloudException {
        List<DataCenter> dataCenters = new ArrayList<DataCenter>();
        for(Region region : listRegions()){
            DataCenter dc = new DataCenter(region.getProviderRegionId() + "-dc", region.getProviderRegionId() + "-dc", region.getProviderRegionId(), true, true);
            dataCenters.add(dc);
        }
        return dataCenters;
    }

    @Override
    public @Nonnull DataCenter getDataCenter(@Nonnull String providerDataCenterId) throws InternalException, CloudException {
        for (Region region : listRegions()){
            for(DataCenter dc : listDataCenters(region.getProviderRegionId())){
                if(dc.getProviderDataCenterId().equals(providerDataCenterId))return dc;
            }
        }
        throw new InternalException("The requested datacenter does not exist or cannot be found");
    }

    @Override
    public @Nonnull Iterable<Region> listRegions() throws InternalException, CloudException {
        HttpUriRequest httpUriRequest = AzureArmRequester.createGetRequest(provider).locations().build();
        ArmProviderModel result = new AzureArmRequester(provider, httpUriRequest).withJsonProcessor(ArmProviderModel.class).execute();

        ArmResourceTypeModel azureArmResourceTypeModel = (ArmResourceTypeModel)CollectionUtils.find(result.getAzureArmResourceTypes(), new Predicate() {
            @Override
            public boolean evaluate(Object object) {
                return ((ArmResourceTypeModel) object).getResourceType().equalsIgnoreCase("virtualMachines");
            }
        });

        if(azureArmResourceTypeModel == null || azureArmResourceTypeModel.getLocations() == null)
            return Collections.emptyList();

        final List<Region> regions = new ArrayList<Region>();
        CollectionUtils.forAllDo(azureArmResourceTypeModel.getLocations(), new Closure() {
            @Override
            public void execute(Object input) {
                regions.add(regionFromString((String) input));
            }
        });

        return regions;
    }

    private Region regionFromString(String locationName) {
        Region region = new Region();
        region.setProviderRegionId(locationName);
        region.setName(locationName);
        region.setActive(true);
        region.setAvailable(true);

        Jurisdiction jurisdiction = Jurisdiction.US;
        if(locationName.contains("Europe"))jurisdiction = Jurisdiction.EU;
        else if(locationName.contains("Asia"))jurisdiction = Jurisdiction.CH;
        else if(locationName.contains("Japan"))jurisdiction = Jurisdiction.JP;
        else if(locationName.contains("Australia"))jurisdiction = Jurisdiction.AU;
        else if(locationName.contains("Brazil"))jurisdiction = Jurisdiction.BR;
        region.setJurisdiction(jurisdiction.name());
        return region;
    }

    @Override
    public @Nonnull Iterable<ResourcePool> listResourcePools(@Nonnull String providerDataCenterId) throws InternalException, CloudException {
        throw new InternalException("Resource pools are not supported in Azure");
    }

    @Override
    public @Nullable ResourcePool getResourcePool(@Nonnull String providerResourcePoolId) throws InternalException, CloudException {
        throw new InternalException("Resource pools are not supported in Azure");
    }

    @Override
    public @Nonnull Iterable<StoragePool> listStoragePools() throws InternalException, CloudException {
        throw new InternalException("Storage pools are not supported in Azure");
    }

    @Override
    public @Nullable StoragePool getStoragePool(@Nonnull String providerStoragePoolId) throws InternalException, CloudException {
        throw new InternalException("Storage pools are not supported in Azure");
    }

    @Override
    public @Nonnull Iterable<Folder> listVMFolders() throws InternalException, CloudException {
        throw new InternalException("VM Folders are not supported in Azure");
    }

    @Override
    public @Nullable Folder getVMFolder(@Nonnull String providerVMFolderId) throws InternalException, CloudException {
        throw new InternalException("VM Folders are not supported in Azure");
    }
}
