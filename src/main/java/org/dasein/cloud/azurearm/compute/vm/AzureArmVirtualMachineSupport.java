package org.dasein.cloud.azurearm.compute.vm;

import org.apache.commons.collections.Closure;
import org.apache.commons.collections.CollectionUtils;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.azurearm.AzureArm;
import org.dasein.cloud.azurearm.compute.vm.model.ArmVirtualMachineModel;
import org.dasein.cloud.azurearm.compute.vm.model.ArmVirtualMachinesModel;
import org.dasein.cloud.compute.AbstractVMSupport;
import org.dasein.cloud.compute.VMLaunchOptions;
import org.dasein.cloud.compute.VirtualMachine;
import org.dasein.cloud.compute.VirtualMachineCapabilities;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;

import static org.dasein.cloud.azurearm.AzureArmRequester.*;

/**
 * Created by vmunthiu on 8/10/2015.
 */
public class AzureArmVirtualMachineSupport extends AbstractVMSupport<AzureArm> {
    private AzureArm provider;

    public AzureArmVirtualMachineSupport(AzureArm provider) {
        super(provider);
        this.provider = provider;
    }

    @Nonnull
    @Override
    public VirtualMachineCapabilities getCapabilities() throws InternalException, CloudException {
        return new AzureArmVirtualMachineCapabilities(this.getProvider());
    }

    @Override
    public boolean isSubscribed() throws CloudException, InternalException {
        return true;
    }

    @Nonnull
    @Override
    public VirtualMachine launch(@Nonnull VMLaunchOptions withLaunchOptions) throws CloudException, InternalException {
        ArmVirtualMachineModel armVirtualMachineModel = new ArmVirtualMachineModel();
        armVirtualMachineModel =
                createPutRequest(provider)
                        .forVirtualMachine(armVirtualMachineModel)
                        .withJsonProcessor(ArmVirtualMachineModel.class)
                        .execute();

        return  virtualMachineFrom(armVirtualMachineModel);
    }

    @Override
    public void stop( @Nonnull String vmId, boolean force ) throws InternalException, CloudException {
        createPostRequest(provider).forStopVirtualMachine(vmId).execute();
    }

    @Override
    public void terminate(@Nonnull String vmId, String explanation) throws InternalException, CloudException {

    }

    @Override
    public @Nonnull Iterable<VirtualMachine> listVirtualMachines() throws InternalException, CloudException {
        final ArmVirtualMachinesModel virtualMachineModels =
                createGetRequest(provider)
                        .forVirtualMachines()
                        .withJsonProcessor(ArmVirtualMachinesModel.class)
                        .execute();

        if(virtualMachineModels == null || virtualMachineModels.getArmVirtualMachineModels() == null)
            return Collections.emptyList();

        final ArrayList<VirtualMachine> virtualMachines = new ArrayList<VirtualMachine>();
        CollectionUtils.forAllDo(virtualMachineModels.getArmVirtualMachineModels(), new Closure() {
            @Override
            public void execute(Object input) {
                virtualMachines.add(virtualMachineFrom((ArmVirtualMachineModel) input));
            }
        });

        return virtualMachines;
    }

    private VirtualMachine virtualMachineFrom(ArmVirtualMachineModel armVirtualMachineModel) {
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.setProviderVirtualMachineId(armVirtualMachineModel.getId());
        virtualMachine.setProviderRegionId(armVirtualMachineModel.getLocation());
        //virtualMachine.setProviderDataCenterId(virtualMachineModel.getStampId());
        virtualMachine.setName(armVirtualMachineModel.getName());
        //virtualMachine.setCurrentState(getVmState(virtualMachineModel.getStatusString()));
        //virtualMachine.setProviderOwnerId(virtualMachineModel.getOwner().getRoleID());
        virtualMachine.setPersistent(true);

        return virtualMachine;
    }
}
