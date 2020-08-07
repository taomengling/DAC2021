package org.cloudbus.cloudsim.entity;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

public class CD_Workload extends Cloudlet {
    //云盘工作负载类，与虚拟机任务类似，
    //云盘工作负载与虚拟机工作负载的不同：
    //1.云盘工作负载的随云盘到期销毁，不存在执行完停止这个过程(云盘是持久级)
    //2.云盘的工作负载维度更多,包括读写IO和读写带宽
    private UtilizationModel utilizationModel_Bw_read;
    private UtilizationModel utilizationModel_Bw_write;
    private UtilizationModel utilizationModel_io_read;
    private UtilizationModel utilizationModel_io_write;

    public UtilizationModel getUtilizationModel_Bw_read() {
        return utilizationModel_Bw_read;
    }

    public UtilizationModel getUtilizationModel_Bw_write() {
        return utilizationModel_Bw_write;
    }

    public UtilizationModel getUtilizationModel_io_read() {
        return utilizationModel_io_read;
    }

    public UtilizationModel getGetUtilizationModel_io_write() {
        return utilizationModel_io_write;
    }

    public void setUtilizationModel_Bw_read(UtilizationModel utilizationModel_Bw_read) {
        this.utilizationModel_Bw_read = utilizationModel_Bw_read;
    }

    public void setUtilizationModel_Bw_write(UtilizationModel utilizationModel_Bw_write) {
        this.utilizationModel_Bw_write = utilizationModel_Bw_write;
    }

    public void setUtilizationModel_io_read(UtilizationModel utilizationModel_io_read) {
        this.utilizationModel_io_read = utilizationModel_io_read;
    }

    public void setUtilizationModel_io_write(UtilizationModel utilizationModel_io_write) {
        this.utilizationModel_io_write = utilizationModel_io_write;
    }

    private long cloudletlength;
    public CD_Workload
            ( final int cloudletId,
                        final long cloudletLength,
                        final int pesNumber,
                        final long cloudletFileSize,
                        final long cloudletOutputSize,
                        final UtilizationModel utilizationModel_write_iops,
                        final UtilizationModel utilizationModelCap,
                        final UtilizationModel utilizationModel_write_bw,
                        final UtilizationModel utilizationModel_read_iops,
                        final UtilizationModel utilizationModel_read_bw){
        super(cloudletId,cloudletLength,pesNumber,
                cloudletFileSize,cloudletOutputSize,utilizationModel_write_iops,
                utilizationModelCap,utilizationModel_write_bw);
        cloudletlength=cloudletLength;
        setUtilizationModel_io_read(utilizationModel_read_iops);
        setUtilizationModel_Bw_read(utilizationModel_read_bw);
        setUtilizationModel_io_write(utilizationModel_write_iops);
        setUtilizationModel_Bw_write(utilizationModel_write_bw);

    }

    //改变云盘工作负载的生命周期,云盘的工作负载随云盘销毁
    @Override
    public boolean isFinished() {
        return false;
    }

    public double getUtilizationOf_wbw(final double time) { return getUtilizationModel_Bw_write().getUtilization(time);}
    public double getUtilizationOf_rbw(final double time) {
        return getUtilizationModel_Bw_read().getUtilization(time);
    }
    public double getUtilizationOf_wio(final double time) { return getGetUtilizationModel_io_write().getUtilization(time); }
    public double getUtilizationOf_rio(final double time) {
        return getUtilizationModel_io_read().getUtilization(time);
    }
}
