package org.cloudbus.cloudsim.examples.CBS.AllocationAlg;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.entity.CBS_Constants;
import org.cloudbus.cloudsim.entity.Warehouse;
import org.cloudbus.cloudsim.examples.CBS.DiskSelectAlg.HeuristicDiskSelection;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.power.lists.PowerVmList;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

import java.util.*;

public class ThresholdBasedMigration extends PowerVmAllocationPolicyMigrationAbstract {
        /**
         /** The static host CPU utilization threshold to detect over utilization.
         * It is a percentage value from 0 to 1
         * that can be changed when creating an instance of the class. */
        private double utilizationThreshold = 0.9;
        private double balanceThreshold= 0.05;
        private double[] cluster_res_util_std_vector;
    /**
     * Instantiates a new PowerVmAllocationPolicyMigrationStaticThreshold.
     *
     * @param hostList the host list
     * @param vmSelectionPolicy the vm selection policy
     * @param utilizationThreshold the utilization threshold
     */
    public ThresholdBasedMigration(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double utilizationThreshold) {
        super(hostList, vmSelectionPolicy);
        setUtilizationThreshold(utilizationThreshold);
    }

    /**
     * 判断仓库是否过载，超过仓库危险负载值后，发出警报
     *
     * @param host 仓库对象
     * @return true, 有过载风险; false 无风险
     */
    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        // 根据阈值判断仓库是否过载
        addHistoryEntry(host, getUtilizationThreshold());
        Warehouse wh = (Warehouse) host;
        double totalRequestedMips = 0;

        double host_cap = wh.getUtilizationCap()/wh.getRam();
        double host_iops = wh.getUtilizationWriteIops()/wh.getWrite_iops_limit();
        double host_bw = wh.getUtilizationWritebw()/wh.getWrite_bw_limit();
        Boolean isOverload = false;
        if (host_cap > getUtilizationThreshold() ||
            host_iops > getUtilizationThreshold() ||
            host_bw > getUtilizationThreshold()){
            isOverload = true;
        }

        return isOverload;
    }
    protected List<PowerHostUtilizationHistory> getUnbalanceWarehouse() {
        List<PowerHostUtilizationHistory> overUtilizedHosts = new LinkedList<PowerHostUtilizationHistory>();
        for (PowerHostUtilizationHistory host : this.<PowerHostUtilizationHistory> getHostList()) {
            if (isUnbalanceWarehouse(host)) {
                overUtilizedHosts.add(host);
            }
        }
        return overUtilizedHosts;
    }
    /**
     * 获得存储集群目前的平均资源使用向量 <>Cap,read_IOPS,read_Bandwidth,Write_IOPS,Write_Bandwidth</>
     * 平均资源使用定义：前n个时间戳 仓库资源利用率的均值
     * @param hosts
     * @return
     */
    public double[] getBalanceStandard(List<Host> hosts){
        double balance = 0;
        double[] balance_all_dim =  new double[5];
        double[][] warehouse_load = new double[hosts.size()][5];
        double[][] selected_wh_array = new double[1][hosts.size()];
        for(int i = 0; i< hosts.size();i++){
            selected_wh_array[0][i] = 1;
        }
        for(int i=0;i< hosts.size();i++){
            Warehouse wh = (Warehouse)hosts.get(i);
            warehouse_load[i] = wh.getWarehouse_Res_Vector();
        }
        RealMatrix load_matrix = new Array2DRowRealMatrix(warehouse_load);
        RealMatrix selected_wh_matrix = new Array2DRowRealMatrix(selected_wh_array);
        RealMatrix system_av_load = selected_wh_matrix.multiply(load_matrix); //[]
        for(int i=0;i<5;i++){
            system_av_load.setEntry(0,i,system_av_load.getEntry(0,i)/hosts.size());
        }
        double [][] balance_standard = system_av_load.getData();
        this.cluster_res_util_std_vector = balance_standard[0];
        return balance_standard[0];

    }

    /**
     * 判断仓库是否处于不均衡状态
     * @param host 仓库对象
     * @return True 处于不均衡状态 false均衡状态
     */
    protected boolean isUnbalanceWarehouse(PowerHost host) {
        // 根据阈值判断仓库是否过载 资源利用率是否大于阈值
        double[] util_std = getBalanceStandard(getHostList());
        addHistoryEntry(host, getBalanceThreshold());
        Warehouse wh = (Warehouse) host;
        double[] wh_util_av = wh.getWarehouse_Res_Vector();
        double host_cap = wh_util_av[0];
        double host_write_iops = wh_util_av[2];
        double host_write_bw = wh_util_av[4];
        Boolean isOverload = false;
        if (host_cap > util_std[0] + getBalanceThreshold() ||
                host_write_iops > util_std[2] + getBalanceThreshold()||
                host_write_bw > util_std[4] + getBalanceThreshold() && wh.getVmsMigratingOut().size() == 0){
            isOverload = true;
        }
        return isOverload;
    }
    protected boolean isMigration_enough(PowerHost host, List<Vm> vm_list) {
        // 根据阈值判断仓库是否过载
        addHistoryEntry(host, getUtilizationThreshold());
        Warehouse wh = (Warehouse) host;
        double vm_cap =0,vm_iops = 0,vm_bw = 0;
        for(int i=0;i<vm_list.size();i++){
            vm_cap += vm_list.get(i).getCurrentAllocatedCap();
            vm_iops += vm_list.get(i).getCurrentAllocatedWrite_IOPS();
            vm_bw += vm_list.get(i).getCurrentAllocatedWrite_Bw();
        }
        double host_cap = (wh.getUtilizationCap()-vm_cap)/wh.getRam();
        double host_iops = (wh.getUtilizationWriteIops()-vm_iops)/wh.getWrite_iops_limit();
        double host_bw = (wh.getUtilizationWritebw()-vm_bw)/wh.getWrite_bw_limit();
        boolean isEnough = true;
        if (host_cap > cluster_res_util_std_vector[0] + getBalanceThreshold() ||
                host_iops > cluster_res_util_std_vector[2] + getBalanceThreshold()||
                host_bw > cluster_res_util_std_vector[4] + getBalanceThreshold()){
            isEnough = false;
        }

        return isEnough;
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setUtilizationThreshold(double utilizationThreshold) {
        this.utilizationThreshold = utilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getUtilizationThreshold() {
        return utilizationThreshold;
    }

    //重载迁移分配函数，只迁移过载的仓库
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        if ((CloudSim.clock()-0.1)% 86400 != 0){
            return null; //每天固定时间执行，如果不在时间点内返回空 控制优化算法的执行时间
        }
        ExecutionTimeMeasurer.start("optimizeAllocationTotal");

        ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
        List<PowerHostUtilizationHistory> overUtilizedHosts = getUnbalanceWarehouse();
        //得到过载主机列表
        getExecutionTimeHistoryHostSelection().add(
                ExecutionTimeMeasurer.end("optimizeAllocationHostSelection"));

        printOverUtilizedHosts(overUtilizedHosts);
        if (overUtilizedHosts.size() == 0 ){
            return null;
        }

//        saveAllocation(); // 得到所有正常云盘对应位置 保存目前的分配方式
        //从过载仓库中选择得到等待迁移虚拟机列表
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
//        restoreAllocation(); 恢复原分配方式
        getExecutionTimeHistoryTotal().add(ExecutionTimeMeasurer.end("optimizeAllocationTotal"));

        return migrationMap;
    }

    //重载获得新放置方案函数，原实验使用Power—Aware的虚拟机分配策略
    @Override
    protected List<Map<String, Object>> getNewVmPlacement(
            List<? extends Vm> vmsToMigrate,
            Set<? extends Host> excludedHosts) {
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        PowerVmList.sortByDiskSizeUtilization(vmsToMigrate);

        for (Vm vm : vmsToMigrate) {
            PowerHost allocatedHost = findHostForVm(vm, excludedHosts);
            if (allocatedHost != null) {
//                allocatedHost.vmCreate(vm); 模拟云盘迁移，省去这一步
                allocatedHost.getVmsMigratingIn().add(vm); //迁移队列加入一块盘，用于控制并发度
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());
                double task_start_delay = 0.0 ; // = 0  FIFO任务执行策略
                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrate.put("start_time", CloudSim.clock()+task_start_delay);
                migrationMap.add(migrate);
            }
        }
        for( Map<String, Object> migrate:migrationMap){
            PowerHost host = (PowerHost) migrate.get("host");
            Vm vm = (Vm) migrate.get("vm");
            host.getVmsMigratingIn().remove(vm);
        }

        return migrationMap;
    }
    //重载云盘分配策略，原实验使用Power—Aware的虚拟机分配策略
    // 云盘重新放置，这里采用随机策略,
    // 限制条件:该仓库可以放下云盘 且仓库迁移不超过并发度
    @Override
    public PowerHost findHostForVm(Vm vm, Set<? extends Host> excludedHosts) {
        double minMetric = Double.MAX_VALUE;
        PowerHost allocatedHost = null;
        if (!vm.isBeingInstantiated()) { //迁移过程中
            ArrayList<PowerHost> valid_host = new ArrayList<PowerHost>();
            for (PowerHost host : this.<PowerHost>getHostList()) {//fliter过程
                if (host.isSuitableForVm(vm) && !excludedHosts.contains(host) ) {
                    //有效仓库的筛选条件:1、仓库有足够空间 2、排除已经过载的仓库 3、满足仓库的迁移并发度控制
                    valid_host.add(host);

                }
            }
            if (valid_host.size() == 0) { //选仓过程随机选仓
                return null;
            } else {
                Random r = new Random(1);
                int rand = r.nextInt(valid_host.size());
                return valid_host.get(rand);
            }
        }
        else {
            PowerVm powerVm=(PowerVm) vm;
            int prehostid=powerVm.getPreHostID();
            return  this.<PowerHost>getHostList().get(prehostid);
        }

    }
    @Override
    protected List<? extends Vm>
    getVmsToMigrateFromHosts(List<PowerHostUtilizationHistory> overUtilizedHosts) {
        List<Vm> vmsToMigrate = new LinkedList<Vm>();
        for (PowerHostUtilizationHistory host : overUtilizedHosts) {
            // 已经产生迁移任务的仓库，应该避免重复产生迁移任务
            if (host.getVmsMigratingOut().size()>0){ // 如果该过载仓库已经有迁出任务，不产生新的迁出任务。
                continue;
            }
            while (true) {
                Vm vm = getVmSelectionPolicy().getVmToMigrate(host); //选择合适盘
                if (vm == null) {
                    break;
                }
                vmsToMigrate.add(vm); //假设完成迁移过程
                vm.setInMigration(true); //假设该盘的状态改为迁移中
//                host.vmDestroy(vm); // 不去模拟云盘迁出，节省恢复分配方式复杂度。
                if (isMigration_enough(host,vmsToMigrate)) { //
                    break;
                }
            }
        }
        for(Vm vm:vmsToMigrate){ // 恢复迁移前状态
            vm.setInMigration(false);
        }
        return vmsToMigrate;
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

    public double getBalanceThreshold() {
        return balanceThreshold;
    }

    public void setBalanceThreshold(double balanceThreshold) {
        this.balanceThreshold = balanceThreshold;
    }
}

