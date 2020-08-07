package org.cloudbus.cloudsim.examples.CBS.MigTaskManagement;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.entity.CloudDisk;
import org.cloudbus.cloudsim.entity.Warehouse;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class Management_Strategy {
    /** 等待迁移的云盘列表*/
    public List<CloudDisk> vms = new ArrayList<>();
    /** 系统中可以接受迁移的仓库列表*/
    public List<Warehouse> hosts = new ArrayList<>();
    /** 由迁移策略产生的重映射关系，mig_map[i] = j  表示第i号云盘装入 第j号仓库*/
    public int[] mig_map;
    /** 迁移任务完成的最晚时间，比如必须在24小时内完成,预计会超出最晚时间的迁移任务会被丢弃*/
    public double task_over_time;
    public Management_Strategy(List<CloudDisk> vmToMigration, List<Warehouse> hostList,int[] mig_plan,double task_over_time) {
        this.vms = vmToMigration;
        this.hosts = hostList;
        this.mig_map = mig_plan;
        this.task_over_time = task_over_time;
    }
    public double[] get_disk_migration_time_fit(Vm  vm){
        CloudDisk cd = (CloudDisk) vm;
        List<Double> util_wbw = cd.getUtilizationHistory_write_bw(); //得到云盘过去history_len长度的历史资源利用率
        double[] fit = new double[util_wbw.size()];
        double util_min = Double.MAX_VALUE;
        double util_max = Double.MIN_VALUE;
        for(double util :util_wbw){
            util_min = Math.min(util_min, util);
            util_max = Math.max(util_max,util);
        }
        for(int i=0;i<util_wbw.size();i++){
            fit[i] = (util_wbw.get(i) - util_min)/(util_max-util_min);
        }
        return fit;
    }
    /**
     * 抽象函数：通过某种策略输出每个任务的执行时间
     * @return 任务执行时间 time[i] 第i个任务的执行时间
     */
    public abstract double[] output_task_start_time();


}
