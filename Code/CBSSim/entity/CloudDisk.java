package org.cloudbus.cloudsim.entity;

import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.util.MathUtil;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CloudDisk  extends PowerVm {


    /** 记录多长时间的历史记录，超过多少丢弃 */
    public static final int HISTORY_LENGTH = 288;


    /** 云盘 iops 的利用率*/
    private final List<Double> utilizationHistory_write_iops = new LinkedList<Double>();

    private final List<Double> utilizationHistory_write_bw = new LinkedList<Double>();

    private final List<Double> utilizationHistory_read_iops = new LinkedList<Double>();

    private final List<Double> utilizationHistory_read_bw = new LinkedList<Double>();

    private final List<Double> utilizationHistoryCap = new LinkedList<Double>();

    /** 上一次IO负载运行时刻 */
    private double previousTime;

    /** 迁移前的仓库ID*/
    private int PreHostID;

    public int getPreHostID() {
        return PreHostID;
    }

    public void setPreHostID(int preHostID) {
        PreHostID = preHostID;
    }

    /** The scheduling interval to update the processing of cloudlets
     * running in this VM. */
    private double schedulingInterval;

    /**
     * Instantiates a new PowerVm.
     *
     * @param id the id
     * @param userId the user id
     * @param mips the 带宽
     * @param pesNumber the 物理机核数(云盘场景下默认为1)
     * @param ram  虚拟机中的内存概念(对应云盘场景下的容量)
     * @param bw the (云盘场景下的带宽)
     * @param size the (暂时没用)
     * @param priority the priority(优先级 赞数没用)
     * @param vmm the vmm(模拟vm监控器，对应数据中心中的iostat监控系统)
     * @param cloudletScheduler the cloudlet scheduler(云任务调度器，管理IO任务向云盘的下发)
     * @param schedulingInterval the scheduling interval(监控系统的间隔，与实验模拟时间间隔类似)
     */
    public CloudDisk(
            final int id,
            final int userId,
            final double mips,
            final int pesNumber,
            final int ram,
            final long bw,
            final long size,
            final int priority,
            final String vmm,
            final CloudletScheduler cloudletScheduler,
            final double schedulingInterval,
            final int preHostID) {
        super(id, userId, mips, pesNumber, ram, bw, size,priority,vmm, cloudletScheduler,preHostID);
        setSchedulingInterval(schedulingInterval);
        setPreHostID(preHostID);
    }


//    public CloudDisk(
//            final int id,
//            final int userId,
//            final double mips,
//            final int pesNumber,
//            final int ram,
//            final long bw,
//            final long size,
//            final int priority,
//            final String vmm,
//            final CloudletScheduler cloudletScheduler,
//            final double schedulingInterval) {
//        super(id, userId, mips, pesNumber, ram, bw, size,priority,vmm,cloudletScheduler);
//        setSchedulingInterval(schedulingInterval);
//    }

    // 每隔一段时间间隔进行云盘实体的更新，包括更新所有维度资源的利用率，云盘历史记录表，云盘状态
    @Override
    public double updateVmProcessing(final double currentTime, final List<Double> mipsShare) {
        double time = super.updateVmProcessing(currentTime, mipsShare);
        if (currentTime > getPreviousTime() && (currentTime - 0.1) % getSchedulingInterval() == 0) {
            Warehouse wh = (Warehouse)getHost();
            double utilization_write_iops = getCurrentAllocatedWrite_IOPS()/wh.getWrite_iops_limit();
            double utilization_write_bw= getCurrentAllocatedWrite_Bw()/wh.getWrite_bw_limit();
            double utilization_cap = getTotalUtilizationOfCap()/wh.getRam();
            double utilization_read_iops = getCurrentAllocatedRead_IOPS()/wh.getRead_iops_limit();
            double utilization_read_bw = getCurrentAllocatedRead_Bw()/wh.getRead_bw_limit();

            if (CloudSim.clock() != 0 || utilization_write_iops != 0 || utilization_write_bw!=0) {
                addUtilizationHistoryValue(utilization_write_iops,utilization_write_bw,utilization_cap,utilization_read_iops,utilization_read_bw);
            }
            setPreviousTime(currentTime);
        }
        return time;
    }

    /**
     * Gets the utilization MAD in MIPS.
     *
     * @return the utilization MAD in MIPS
     */
    public double getUtilizationMad() {
        double mad = 0;
        if (!getUtilizationHistory_write_bw().isEmpty()) {
            int n = HISTORY_LENGTH;
            if (HISTORY_LENGTH > getUtilizationHistory_write_bw().size()) {
                n = getUtilizationHistory_write_bw().size();
            }
            double median = MathUtil.median(getUtilizationHistory_write_bw());
            double[] deviationSum = new double[n];
            for (int i = 0; i < n; i++) {
                deviationSum[i] = Math.abs(median - getUtilizationHistory_write_bw().get(i));
            }
            mad = MathUtil.median(deviationSum);
        }
        return mad;
    }

    /**
     * Gets the utilization mean in percents.
     *
     * @return the utilization mean in MIPS
     */
    public double[] getAllDimUtilizationMean() {
        double mean_cap = 0,mean_read_iops = 0,mean_write_iops = 0,mean_read_bw = 0,mean_write_bw = 0;
        if (!getUtilizationHistory_write_bw().isEmpty()) {
            int n = HISTORY_LENGTH;
            if (HISTORY_LENGTH > getUtilizationHistory_write_bw().size()) {
                n = getUtilizationHistory_write_bw().size();
            }
            for (int i = 0; i < n; i++) {
                mean_cap += (double) getRam()/getHost().getRam();
                mean_read_iops += getUtilizationHistory_read_iops().get(i);
                mean_write_iops += getUtilizationHistory_write_iops().get(i);
                mean_read_bw += getUtilizationHistory_read_bw().get(i);
                mean_write_bw += getUtilizationHistory_write_bw().get(i);
            }
            mean_cap /= n;
            mean_read_iops /= n;
            mean_write_iops /= n;
            mean_read_bw /= n;
            mean_write_bw /= n;
        }
        return new double[]{mean_cap,mean_read_iops,mean_write_iops,mean_read_bw,mean_write_bw};
    }

    /**
     * Gets the utilization variance in MIPS.
     *
     * @return the utilization variance in MIPS
     */
    public double getUtilizationVariance() {
        double mean = getUtilizationMean();
        double variance = 0;
        if (!getUtilizationHistory_write_bw().isEmpty()) {
            int n = HISTORY_LENGTH;
            if (HISTORY_LENGTH > getUtilizationHistory_write_bw().size()) {
                n = getUtilizationHistory_write_bw().size();
            }
            for (int i = 0; i < n; i++) {
                double tmp = getUtilizationHistory_write_bw().get(i) * getMips() - mean;
                variance += tmp * tmp;
            }
            variance /= n;
        }
        return variance;
    }

    /**
     * Adds a CPU utilization percentage history value.
     *
     * @param //utilization the CPU utilization percentage to add
     */
    public void addUtilizationHistoryValue(final double utilization_write_iops,final double utilization_write_bw,
                                           final double utilization_cap,final double utilization_read_iops,
                                           final double utilization_read_bw) {
        getUtilizationHistory_write_iops().add(0, utilization_write_iops);
        getUtilizationHistory_write_bw().add(0, utilization_write_bw);
        getUtilizationHistoryCap().add(0,utilization_cap);
        getUtilizationHistory_read_iops().add(0,utilization_read_iops);
        getUtilizationHistory_read_bw().add(0,utilization_read_bw);
        if (getUtilizationHistory_write_iops().size() > HISTORY_LENGTH ) {
            getUtilizationHistory_write_iops().remove(HISTORY_LENGTH);
        }
        if (getUtilizationHistory_write_bw().size() > HISTORY_LENGTH ) {
            getUtilizationHistory_write_bw().remove(HISTORY_LENGTH);
        }
        if (getUtilizationHistoryCap().size() > HISTORY_LENGTH ) {
            getUtilizationHistoryCap().remove(HISTORY_LENGTH);
        }
        if (getUtilizationHistory_read_iops().size() > HISTORY_LENGTH ) {
            getUtilizationHistory_read_iops().remove(HISTORY_LENGTH);
        }
        if (getUtilizationHistory_read_bw().size() > HISTORY_LENGTH ) {
            getUtilizationHistory_read_bw().remove(HISTORY_LENGTH);
        }
    }

    /**
     * Gets the CPU utilization percentage history.
     *
     * @return the CPU utilization percentage history
     */


    public List<Double> getUtilizationHistoryCap() {
        return utilizationHistoryCap;
    }

    public List<Double> getUtilizationHistory_write_iops() {
        return utilizationHistory_write_iops;
    }

    public List<Double> getUtilizationHistory_write_bw() {
        return utilizationHistory_write_bw;
    }


    public List<Double> getUtilizationHistory_read_iops() {
        return utilizationHistory_read_iops;
    }

    public List<Double> getUtilizationHistory_read_bw() {
        return utilizationHistory_read_bw;
    }

    /**
     * Gets the previous time.
     *
     * @return the previous time
     */
    public double getPreviousTime() {
        return previousTime;
    }

    /**
     * Sets the previous time.
     *
     * @param previousTime the new previous time
     */
    public void setPreviousTime(final double previousTime) {
        this.previousTime = previousTime;
    }

    /**
     * Gets the scheduling interval.
     *
     * @return the schedulingInterval
     */
    public double getSchedulingInterval() {
        return schedulingInterval;
    }

    /**
     * Sets the scheduling interval.
     *
     * @param schedulingInterval the schedulingInterval to set
     */
    protected void setSchedulingInterval(final double schedulingInterval) {
        this.schedulingInterval = schedulingInterval;
    }

}
