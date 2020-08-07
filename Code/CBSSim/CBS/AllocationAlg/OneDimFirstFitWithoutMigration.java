package org.cloudbus.cloudsim.examples.CBS.AllocationAlg;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicyAbstract;

import java.util.List;
import java.util.Map;

public class OneDimFirstFitWithoutMigration extends PowerVmAllocationPolicyAbstract {
    /**
     * Instantiates a new PowerVmAllocationPolicySimple.
     *
     * @param list the list
     */
    public OneDimFirstFitWithoutMigration(List<? extends Host> list) {
        super(list);
    }

    /**
     * The method doesn't perform any VM allocation optimization
     * and in fact has no effect.
     * @param vmList
     * @return
     */
    //重载迁移策略函数
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        //@todo It is better to return an empty map in order to avoid NullPointerException or extra null checks
        // This policy does not optimize the VM allocation
        //无迁移策略
        return null;
    }
    @Override
    public PowerHost findHostForVm(Vm vm) {
        PowerVm powerVm=(PowerVm)vm;
        int prehostid=powerVm.getPreHostID();
        for (PowerHost host : this.<PowerHost> getHostList()) {
            if (host.isSuitableForVm(vm) && host.getId()==prehostid) {
                return host;
            }
        }
        return null;
    }

}
