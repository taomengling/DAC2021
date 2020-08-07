package org.cloudbus.cloudsim.entity;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;

import java.util.List;
import java.util.Random;

public class CBSDatacenter  extends PowerDatacenter {

    public CBSDatacenter(
            String name,
            DatacenterCharacteristics characteristics,
            VmAllocationPolicy vmAllocationPolicy,
            List<Storage> storageList,
            double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
    }

    @Override
    protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
        double currentTime = CloudSim.clock();
        double minTime = Double.MAX_VALUE;
        double timeDiff = currentTime - getLastProcessTime();
        double timeFrameDatacenterEnergy = 0.0;
        Random r = new Random();
//        if (Math.random()>0.9){
//            int DestIndex = r.nextInt(getHostList().size());
//            CreateNewCloudDisk(50,DestIndex);
//        }
        Log.printLine("\n\n--------------------------------------------------------------\n\n");
        Log.formatLine("New resource usage for the time frame starting at %.2f:", currentTime);

        for (PowerHost host : this.<PowerHost> getHostList()) {
            Log.printLine();

            double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
            if (time < minTime) {
                minTime = time;
            }
            Warehouse wh = (Warehouse) host;
            Log.formatLine(
                    "%.2f: [Host #%d] WriteIOPSUtil is %.2f%% WriteBWUtil is %.2f%% CapUtil is %.2f%% ReadIOPSUtil is %.2f%% ReadBWUtil is %.2f%% ",
                    currentTime,
                    host.getId(),
//                    host.getUtilizationOfCpu() * 100,
                    wh.getUtilizationWriteIops()/wh.getWrite_iops_limit()*100,
                    wh.getUtilizationWritebw()/wh.getWrite_bw_limit()*100,
                    wh.getUtilizationCap()/wh.getRam()*100,
                    wh.getUtilizationReadIops()/wh.getRead_iops_limit()*100,
                    wh.getUtilizationReadbw()/wh.getRead_bw_limit()*100
                    );
        }

//        if (timeDiff > 0) {
//            Log.formatLine(
//                    "\nEnergy consumption for the last time frame from %.2f to %.2f:",
//                    getLastProcessTime(),
//                    currentTime);
//
//            for (PowerHost host : this.<PowerHost> getHostList()) {
//                double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
//                double utilizationOfCpu = host.getUtilizationOfCpu();
//                double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
//                        previousUtilizationOfCpu,
//                        utilizationOfCpu,
//                        timeDiff);
//                timeFrameDatacenterEnergy += timeFrameHostEnergy;
//
//                Log.printLine();
//                Log.formatLine(
//                        "%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
//                        currentTime,
//                        host.getId(),
//                        getLastProcessTime(),
//                        previousUtilizationOfCpu * 100,
//                        utilizationOfCpu * 100);

//                Log.formatLine(
//                        "%.2f: [Host #%d] energy is %.2f W*sec",
//                        currentTime,
//                        host.getId(),
//                        timeFrameHostEnergy);
//            }

//            Log.formatLine(
//                    "\n%.2f: Data center's energy is %.2f W*sec\n",
//                    currentTime,
//                    timeFrameDatacenterEnergy);
//        }

        setPower(getPower() + timeFrameDatacenterEnergy);

        checkCloudletCompletion();

        /** Remove completed VMs **/
        for (PowerHost host : this.<PowerHost> getHostList()) {
            for (Vm vm : host.getCompletedVms()) {
                getVmAllocationPolicy().deallocateHostForVm(vm);
                getVmList().remove(vm);
                Log.printLine("VM #" + vm.getId() + " has been deallocated from host #" + host.getId());
            }
        }

        Log.printLine();

        setLastProcessTime(currentTime);
        return minTime;
    }

    /**
     * 在系统运行时创建新云盘
     * @param disk_cap 云盘的容量大小
     * @param DestWarehouseIndex 云盘装箱的目的仓库
     */
    public void CreateNewCloudDisk(int disk_cap,int DestWarehouseIndex){
        UtilizationModel utilizationModelnull = new UtilizationModelNull();
        UtilizationModel utilizationModelFull = new UtilizationModelFull();
        int brokerId = -1;
        Cloudlet cloudlet = new CD_Workload(
                this.getVmList().size(), //编号为目前虚拟机列表的大小，实现自增，云任务和虚拟机一一对应
                CBS_Constants.CLOUDLET_LENGTH,
                CBS_Constants.CLOUDLET_PES,
                300,
                300,
                utilizationModelnull,
                utilizationModelFull,
                utilizationModelnull,
                utilizationModelnull,
                utilizationModelnull);
        cloudlet.setUserId(brokerId);
        cloudlet.setVmId(this.getVmList().size()-1);
        Vm vm = new CloudDisk(
                this.getVmList().size(), // 编号为目前虚拟机列表的大小，自增id
                brokerId,
                CBS_Constants.VM_MIPS[1],
                CBS_Constants.VM_PES[1],
                ((int) disk_cap),
                CBS_Constants.VM_BW[1],
                CBS_Constants.VM_SIZE,
                1,
                "Xen",
                new CD_CloudletSchedulerDynamicWorkload(CBS_Constants.VM_MIPS[1], CBS_Constants.VM_PES[1]),
                CBS_Constants.SCHEDULING_INTERVAL,
                DestWarehouseIndex);

        if (getVmAllocationPolicy().allocateHostForVm(vm)) { // 判断是否有足够容量分配该云盘
            getVmList().add(vm); //数据中心云盘列表记录 添加新云盘
            getHostList().get(DestWarehouseIndex).getVmList().add(vm); //仓库云盘列表添加新云盘
            if (vm.isBeingInstantiated()) {
                vm.setBeingInstantiated(false); // 更改云盘状态为已挂载
            }
            vm.getCloudletScheduler().cloudletSubmit(cloudlet,0); // 提交云任务到云任务调度器
            vm.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(vm).getVmScheduler()
                    .getAllocatedMipsForVm(vm)); // 更新云盘的状态
        }
    }

}
