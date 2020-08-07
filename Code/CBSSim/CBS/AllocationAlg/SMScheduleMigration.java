package org.cloudbus.cloudsim.examples.CBS.AllocationAlg;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.entity.CloudDisk;
import org.cloudbus.cloudsim.entity.Warehouse;
import org.cloudbus.cloudsim.examples.CBS.DiskSelectAlg.DiskGroupSelection;
import org.cloudbus.cloudsim.examples.CBS.HeuristicAlg.Modified_GA;
import org.cloudbus.cloudsim.examples.CBS.HeuristicAlg.Modified_SA;
import org.cloudbus.cloudsim.examples.CBS.MigTaskManagement.ScheduleFIFO;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;

import java.util.*;

public class SMScheduleMigration extends PowerVmAllocationPolicyMigrationAbstract {
    /**
     /** The static host CPU utilization threshold to detect over utilization.
     * It is a percentage value from 0 to 1
     * that can be changed when creating an instance of the class. */
    /**
     * 均衡度阈值，当资源利用率超过产品均值多少时触发迁移
     */
    private double balanceThreshold ;

    /**
     * Instantiates a new PowerVmAllocationPolicyMigrationStaticThreshold.
     *
     * @param hostList the host list
     * @param vmSelectionPolicy the vm selection policy
     * @param utilizationThreshold the utilization threshold
     */
    public SMScheduleMigration(
            List<? extends Host> hostList,
            PowerVmSelectionPolicy vmSelectionPolicy,
            double utilizationThreshold) {
        super(hostList, vmSelectionPolicy);

        setBalanceThreshold(utilizationThreshold);
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
        return balance_standard[0];

    }

    /**
     *
     * @return
     */

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
     *  选择高于系统平均负载度一定范围的仓库进行迁出(迁移触发条件，有的仓库偏离产品均值)
     *
     * @param host the host
     * @return true, if the host is 不均衡; false otherwise
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
        if ((host_cap -util_std[0]) > getBalanceThreshold() ||
                (host_write_iops -util_std[2]) > getBalanceThreshold()||
                (host_write_bw - util_std[4]) > getBalanceThreshold() && wh.getVmsMigratingOut().size() == 0){
            isOverload = true;
        }
        return isOverload;
    }


    /**
     *  选择高于仓库负载阈值的仓库进行迁出。
     *
     * @param host the host
     * @return true, if the host is over utilized; false otherwise
     */
    @Override
    protected boolean isHostOverUtilized(PowerHost host) {
        // 根据阈值判断仓库是否过载 资源利用率是否大于阈值

        addHistoryEntry(host, getBalanceThreshold());
        Warehouse wh = (Warehouse) host;
        double host_cap = wh.getUtilizationCap()/wh.getRam();
        double host_write_iops = wh.getUtilizationWriteIops()/wh.getWrite_iops_limit();
        double host_write_bw = wh.getUtilizationWritebw()/wh.getWrite_bw_limit();
        Boolean isOverload = false;
        if (host_cap > getBalanceThreshold()  ||
                host_write_iops > getBalanceThreshold() ||
                host_write_bw > getBalanceThreshold()){
            isOverload = true;
        }

        return isOverload;
    }



    protected boolean isMigration_enough(PowerHost host, List<Vm> vm_list) {
        // 判断当前的迁移任务是否迁移足够多的云盘
        addHistoryEntry(host, getBalanceThreshold());
        Warehouse wh = (Warehouse) host;
        double vm_cap =0,vm_iops = 0,vm_bw = 0;
        for(int i=0;i<vm_list.size();i++){
            vm_cap += vm_list.get(i).getCurrentAllocatedCap();
            vm_iops += vm_list.get(i).getCurrentAllocatedWrite_IOPS();
            vm_bw += vm_list.get(i).getCurrentAllocatedWrite_Bw();
        }
        double host_cap = (wh.getUtilizationCap()-vm_cap)/wh.getRam();
        double host_iops = (wh.getUtilizationWriteIops()-vm_iops)/wh.getWrite_iops_limit();
        double host_bw = (wh.getUtilizationOfBw()-vm_bw)/wh.getWrite_bw_limit();
        boolean isOverload = false;
        if (host_cap > getBalanceThreshold() ||
                host_iops > getBalanceThreshold() ||
                host_bw > getBalanceThreshold()){
            isOverload = true;
        }

        return isOverload;
    }

    /**
     * Sets the utilization threshold.
     *
     * @param balanceThreshold the new utilization threshold
     */
    protected void setBalanceThreshold(double balanceThreshold) {
        this.balanceThreshold = balanceThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getBalanceThreshold() {
        return balanceThreshold;
    }

    //重载迁移分配函数，通过启发式算法优化布局
    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
        if ((CloudSim.clock()-0.1)% 86400 != 0){
            return null; //每天固定时间执行，如果不在时间点内返回空 控制退火算法的执行时间
        }
        ExecutionTimeMeasurer.start("optimizeAllocationTotal");

        ExecutionTimeMeasurer.start("optimizeAllocationHostSelection");
//        List<PowerHostUtilizationHistory> overUtilizedHosts = getOverUtilizedHosts();
        List<PowerHostUtilizationHistory> overUtilizedHosts = getUnbalanceWarehouse(); //得到处于高负载的仓库
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
        // List<? extends Vm> vmsToMigrate = getVmsToMigrateFromHosts(overUtilizedHosts);
        List<? extends Vm> vmsToMigrate = getVmsAbleToMigration(overUtilizedHosts);
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
        List<Warehouse> warehouses = new ArrayList<>();
        List<CloudDisk> cloudDisks = new ArrayList<>();
        List<Map<String, Object>> migrationMap = new LinkedList<Map<String, Object>>();
        if (vmsToMigrate.size() == 0){
            return null;
        }
        for(Host host:getHostList()){
            warehouses.add((Warehouse)host);
        }
        for(Vm vm:vmsToMigrate){
            cloudDisks.add((CloudDisk)vm);
        }
//        Modified_SA SA = new Modified_SA(cloudDisks,warehouses); // 模拟退火算法 cloudDisks为可能迁移的云盘，warehouses所有可以用于迁移的仓库
//        int[] Best_map = SA.minimizeCostBeyondConstraint(); // 模拟退火产生的最佳迁移计划
        Modified_GA GA = new Modified_GA(cloudDisks,warehouses);
        int[] Best_map = GA.calculate();
        ScheduleFIFO scheduleFIFO =  new ScheduleFIFO(cloudDisks,warehouses,Best_map,CloudSim.clock()+86400);
        double[] task_schedule =  scheduleFIFO.output_task_start_time();     // 增加任务调度功能，将迁移任务调度到合适时间执行 FIFO 产生任务的时刻后立即执行，任务会按照规则排序

        double now_time = CloudSim.clock();
//        PowerVmList.sortByDiskSizeUtilization(vmsToMigrate);
        for (int i=0;i<vmsToMigrate.size();i++) {
            Vm vm = vmsToMigrate.get(i);
            PowerHost allocatedHost = (PowerHost) getHostList().get(Best_map[i]);
            if (allocatedHost == vm.getHost()){
                continue;
            }
            if (allocatedHost != null && allocatedHost != vm.getHost()) {
                Log.printConcatLine("VM #", vm.getId(), " allocated to host #", allocatedHost.getId());
                Map<String, Object> migrate = new HashMap<String, Object>();
                migrate.put("vm", vm);
                migrate.put("host", allocatedHost);
                migrate.put("start_time", task_schedule[i]);
                migrationMap.add(migrate);
            }
        }
        return migrationMap;
    }

    /**
     *   重载云盘分配策略，原实验使用Power—Aware的虚拟机分配策略
     *   云盘重新放置，这里采用模拟退火策略
     *   限制条件:该仓库可以放下云盘 且仓库迁移不超过并发度
     * @param vm the VM
     * @param excludedHosts the excluded hosts
     * @return
     */

    @Override
    public PowerHost findHostForVm(Vm vm, Set<? extends Host> excludedHosts) {
        double minMetric = Double.MAX_VALUE;
        PowerHost allocatedHost = null;
        if (!vm.isBeingInstantiated()) { //迁移过程中
            ArrayList<PowerHost> valid_host = new ArrayList<PowerHost>();
            for (PowerHost host : this.<PowerHost>getHostList()) {//fliter过程
                if (host.isSuitableForVm(vm)) {
                    //有效仓库的筛选条件:1、仓库有足够空间
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

    protected List<? extends Vm>
    getVmsAbleToMigration(List<PowerHostUtilizationHistory> overUtilizedHosts) {
        List<Vm> vmsToMigrate = new LinkedList<Vm>();
        DiskGroupSelection selection = (DiskGroupSelection)getVmSelectionPolicy();
        for (PowerHostUtilizationHistory host : overUtilizedHosts) {
            // 已经产生迁移任务的仓库，应该避免重复产生迁移任务
            //int  suit_disk_num = host.getVmList().size()/100;
            int  suit_disk_num = 200;
            int count = 0;
            if (host.getVmsMigratingOut().size()>0){ // 如果该过载仓库已经有迁出任务，不产生新的迁出任务。
                continue;
            }
            selection.sortVmByPrior(host); //按照优先级排序
            while (true) {
                Vm vm = selection.getVmToMigrate(host); // 从优先级队列中选择合适盘
                if (vm == null) {
                    break;
                }
                vmsToMigrate.add(vm); //假设完成迁移过程
                vm.setInMigration(true); //假设该盘的状态改为迁移中
//                host.vmDestroy(vm); // 不去模拟云盘迁出，节省恢复分配方式复杂度。
                if (count>=suit_disk_num) {
                    break;
                }
                count++;
            }
        }
        for(Vm vm:vmsToMigrate){ // 恢复迁移前状态
            vm.setInMigration(false);
        }
        return vmsToMigrate;
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
                if (!isMigration_enough(host,vmsToMigrate)) {
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
            balance_metric = host.getAvailableMips();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return -balance_metric;
    }
}
