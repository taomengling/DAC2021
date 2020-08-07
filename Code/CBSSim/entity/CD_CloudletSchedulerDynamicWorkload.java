package org.cloudbus.cloudsim.entity;

import org.cloudbus.cloudsim.CloudletSchedulerDynamicWorkload;
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.ResCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.List;

public class CD_CloudletSchedulerDynamicWorkload extends CloudletSchedulerDynamicWorkload {

    //云盘工作负载调度器，保证云盘的持久级工作，将云盘工作负载的销毁时间定义为无限长，随云盘的卸载消失。

    public CD_CloudletSchedulerDynamicWorkload(double mips, int numberOfPes){
        super(mips,numberOfPes);
    }
    @Override
    public double updateVmProcessing(double currentTime, List<Double> mipsShare) {
        setCurrentMipsShare(mipsShare);

        double timeSpan = currentTime - getPreviousTime();
        double nextEvent = Double.MAX_VALUE;
        List<ResCloudlet> cloudletsToFinish = new ArrayList<ResCloudlet>();

        for (ResCloudlet rcl : getCloudletExecList()) {
            rcl.updateCloudletFinishedSoFar((long) (timeSpan
                    * getTotalCurrentAllocatedMipsForCloudlet(rcl, getPreviousTime()) * Consts.MILLION));

            if (rcl.getRemainingCloudletLength() == 0) { // finished: remove from the list
                //cloudletsToFinish.add(rcl);
                continue;
            } else { // not finish: estimate the finish time
                double estimatedFinishTime = getEstimatedFinishTime(rcl, currentTime);
                if (estimatedFinishTime - currentTime < CloudSim.getMinTimeBetweenEvents()) {
                    estimatedFinishTime = currentTime + CloudSim.getMinTimeBetweenEvents();
                }
                if (estimatedFinishTime < nextEvent) {
                    nextEvent = estimatedFinishTime;
                }
            }
        }

        for (ResCloudlet rgl : cloudletsToFinish) {
            getCloudletExecList().remove(rgl);
            cloudletFinish(rgl);
        }

        setPreviousTime(currentTime);

        if (getCloudletExecList().isEmpty()) {
            return 0;
        }

        return nextEvent;
    }

//    public double getCurrentRequestedUtilizationOfWrite_IOPS() {
//        double w_iops = 0;
//        for (ResCloudlet cloudlet : cloudletExecList) {
//            CD_Workload cd_workload = (CD_Workload)cloudlet.getCloudlet();
//            w_iops += cd_workload.getUtilizationOf_wio(CloudSim.clock());
//        }
//        return w_iops;
//    }
//    public double getCurrentRequestedUtilizationOfRead_Iops() {
//        double r_iops = 0;
//        for (ResCloudlet cloudlet : cloudletExecList) {
//            CD_Workload cd_workload = (CD_Workload)cloudlet.getCloudlet();
//            r_iops += cd_workload.getUtilizationOf_rio(CloudSim.clock());
//        }
//        return r_iops;
//    }
//    public double getCurrentRequestedUtilizationOfRead_Bw() {
//        double r_bw = 0;
//        for (ResCloudlet cloudlet : cloudletExecList) {
//            CD_Workload cd_workload = (CD_Workload)cloudlet.getCloudlet();
//            r_bw += cd_workload.getUtilizationOf_rbw(CloudSim.clock());
//        }
//        return r_bw;
//    }
//    public double getCurrentRequestedUtilizationOfWrite_Bw() {
//        double r_iops = 0;
//        for (ResCloudlet cloudlet : cloudletExecList) {
//            CD_Workload cd_workload = (CD_Workload)cloudlet.getCloudlet();
//            r_iops += cd_workload.getUtilizationOf_wbw(CloudSim.clock());
//        }
//        return r_iops;
//    }


}


