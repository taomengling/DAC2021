package org.cloudbus.cloudsim.examples.CBS.VmMigrationAlg;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.power.lists.PowerVmList;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

import java.util.*;

public class OneDimBalanceFitLRMigration extends PowerVmAllocationPolicyMigrationLocalRegression {

    public OneDimBalanceFitLRMigration(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double safetyParameter,
            double schedulingInterval,
            PowerVmAllocationPolicyMigrationAbstract fallbackVmAllocationPolicy) {
        super(hostList, vmSelectionPolicy,safetyParameter,schedulingInterval,fallbackVmAllocationPolicy);

    }
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        ExecutionTimeMeasurer.start("optimizeAllocationTotal");

        ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
        List<PowerHostUtilizationHistory> overUtilizedHosts = getOverUtilizedHosts();
        //得到过载主机列表
        getExecutionTimeHistoryHostSelection().add(
                ExecutionTimeMeasurer.end("optimizeAllocationHostSelection"));

        printOverUtilizedHosts(overUtilizedHosts);

        saveAllocation();
        //得到等待迁移虚拟机列表
        ExecutionTimeMeasurer.start("optimizeAllocationVmSelection");
        List<? extends Vm> vmsToMigrate = getVmsToMigrateFromHosts(overUtilizedHosts);
        getExecutionTimeHistoryVmSelection().add(ExecutionTimeMeasurer.end("optimizeAllocationVmSelection"));

        //得到迁移后的分配方式
        Log.printLine("Reallocation of VMs from the over-utilized hosts:");
        ExecutionTimeMeasurer.start("optimizeAllocationVmReallocation");
        List<Map<String, Object>> migrationMap = getNewVmPlacement(vmsToMigrate, new HashSet<Host>(
                overUtilizedHosts));
        getExecutionTimeHistoryVmReallocation().add(
                ExecutionTimeMeasurer.end("optimizeAllocationVmReallocation"));
        Log.printLine();

        //migrationMap.addAll(getMigrationMapFromUnderUtilizedHosts(overUtilizedHosts));
        //完成迁移
        restoreAllocation();

        getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

        return migrationMap;
    }

    //重载获得新放置方案函数，原实验使用Power—Aware的虚拟机分配策略
    @Override
    protected List<Map<String, Object>> getNewVmPlacement(
            List<? extends Vm> vmsToMigrate,
            Set<? extends Host> excludedHosts) {
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        PowerVmList.sortByCpuUtilization(vmsToMigrate);
        for (Vm vm : vmsToMigrate) {
            PowerHost allocatedHost = findHostForVm(vm, excludedHosts);
            if (allocatedHost != null) {
                allocatedHost.vmCreate(vm);
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());

                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrationMap.add(migrate);
            }
        }
        return migrationMap;
    }
    //重载云盘分配策略，原实验使用Power—Aware的虚拟机分配策略
    @Override
    public PowerHost findHostForVm(Vm vm, Set<? extends Host> excludedHosts) {
        double minMetric = Double.MAX_VALUE;
        PowerHost allocatedHost = null;

        for (PowerHost host : this.<PowerHost> getHostList()) {
            if (excludedHosts.contains(host)) {
                continue;
            }//如果是迁移阶段，则排除目前的位置
            if (vm.isBeingInstantiated()){
                PowerVm powerVm=(PowerVm) vm;
                int prehostid=powerVm.getPreHostID();
                if (host.getId()==prehostid){
                    return host;
                }
                else{
                    continue;
                }
            }
            if (host.isSuitableForVm(vm)) {
                if (getUtilizationOfCpuMips(host) != 0 && isHostOverUtilizedAfterAllocation(host, vm)) {
                    continue;
                }
                try {
                    double balanceAfterAllocation = getBalanceAfterAllocation(host, vm);
                    if (balanceAfterAllocation != -1) {
                        double balance_metric = balanceAfterAllocation;
                        if (balance_metric < minMetric) {
                            minMetric = balance_metric;
                            allocatedHost = host;
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return allocatedHost;
    }
    protected double getBalanceAfterAllocation(PowerHost host, Vm vm) {
        double balance_metric = 0;
        try {
            balance_metric=host.getAvailableMips();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return -balance_metric;
    }


}

