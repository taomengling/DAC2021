package org.cloudbus.cloudsim.entity;

import org.cloudbus.cloudsim.HostStateHistoryEntry;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Warehouse extends PowerHostUtilizationHistory {

    private double write_iops_limit = 0;

    public double getWrite_iops_limit() {
        return write_iops_limit;
    }

    public void setWrite_iops_limit(double write_iops_limit) {
        this.write_iops_limit = write_iops_limit;
    }

    public double getWrite_bw_limit() {
        return write_bw_limit;
    }

    public void setWrite_bw_limit(double write_bw_limit) {
        this.write_bw_limit = write_bw_limit;
    }

    private double write_bw_limit = 0;

    private double read_iops_limit = 0;

    private double read_bw_limit = 0;

    public double getRead_iops_limit() {
        return read_iops_limit;
    }

    public void setRead_iops_limit(double read_iops_limit) {
        this.read_iops_limit = read_iops_limit;
    }

    public double getRead_bw_limit() {
        return read_bw_limit;
    }

    public void setRead_bw_limit(double read_bw_limit) {
        this.read_bw_limit = read_bw_limit;
    }

    /**
     * 得到过去一天内，仓库的平均负载<cap,read_iops,write_iops,read_bw,write_bw>
     * @return 资源利用率向量
     */
    public double[] getWarehouse_Res_Vector(){
        int history_len = 288;
        double av_cap_util=0,av_read_iops = 0,av_read_bw = 0,av_write_iops = 0,av_write_bw = 0;
        List<HostStateHistoryEntry> historyEntries = getStateHistory();
        int start_index = Math.max(0,historyEntries.size()-history_len);
        for(int i=start_index;i<historyEntries.size();i++){
            HostStateHistoryEntry state = historyEntries.get(i);
            double cap_util = state.getRequestedCap()/getRam();
            double read_iops = state.getRequestedReadIOPS()/getRead_iops_limit();
            double read_bw = state.getRequestedReadBandwidth()/getRead_bw_limit();
            double write_iops = state.getRequestedWriteIOPS()/getWrite_iops_limit();
            double write_bw = state.getRequestedWriteBandwidth()/getWrite_bw_limit();
            // 计算 仓库平均负载 需要间
//            double read_iops = (state.getRequestedReadIOPS() - getVmsMigratingOut().size()*CBS_Constants.migration_bw)/getRead_iops_limit();
//            double read_bw = (state.getRequestedReadBandwidth() - getVmsMigratingIn().size()*CBS_Constants.migration_bw*1000)/getRead_bw_limit();
//            double write_iops = (state.getRequestedWriteIOPS() - getVmsMigratingIn().size()*CBS_Constants.migration_bw)/getWrite_iops_limit();
//            double write_bw = (state.getRequestedWriteBandwidth() - getVmsMigratingIn().size() * CBS_Constants.migration_bw*1000)/getWrite_bw_limit();
            av_cap_util += cap_util;
            av_read_iops +=read_iops;
            av_read_bw +=read_bw;
            av_write_iops += write_iops;
            av_write_bw += write_bw;
        }
        av_cap_util /= history_len;
        av_read_iops /= history_len;
        av_read_bw /= history_len;
        av_write_iops /= history_len;
        av_write_bw /= history_len;

        return new double[]{av_cap_util,av_read_iops,av_write_iops,av_read_bw,av_write_bw};
    }
    public Warehouse(
            int id,
            RamProvisioner ramProvisioner,
            BwProvisioner bwProvisioner,
            long storage,
            List<? extends Pe> peList,
            VmScheduler vmScheduler,
            PowerModel powerModel,
            double Write_IOPS_limit,
            double Write_BW_limit,
            double Read_IOPS_limit,
            double Read_BW_limit){
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);
        setWrite_iops_limit(Write_IOPS_limit);
        setWrite_bw_limit(Write_BW_limit);
        setRead_iops_limit(Read_IOPS_limit);
        setRead_bw_limit(Read_BW_limit);
    }

}
