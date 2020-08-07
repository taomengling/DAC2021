package org.cloudbus.cloudsim.examples.CBS.MigTaskManagement;

import org.apache.commons.math3.analysis.function.Min;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.entity.CBS_Constants;
import org.cloudbus.cloudsim.entity.CloudDisk;
import org.cloudbus.cloudsim.entity.Warehouse;

import java.util.List;
import java.util.Map;

public class ScheduleFIFO extends Management_Strategy{

    public ScheduleFIFO(List<CloudDisk> vmToMigration, List<Warehouse> hostList , int[] mig_plan, double task_over_time){
        super(vmToMigration,hostList,mig_plan,task_over_time);
    }
    @Override
    public double[] output_task_start_time(){
        double[] start_time = new double[vms.size()]; // 迁移开始时间数组，包含每个任务的具体开始时间
        double[] eval_end_time = new double[vms.size()]; // 迁移估计结束时间，包含每个任务的估计结束时间
        double[][] warehouses_concur = new double[2][hosts.size()];
        for(int i=0;i<start_time.length;i++){
            double min_start_time = Double.MAX_VALUE;
            start_time[i] =  CloudSim.clock(); // 迁移任务时间定为某个任务提交后立即执行
//            double eval_exec_time =vm.getRam()*1000.0/ CBS_Constants.migration_bw; // 执行时间 = 云盘size / 迁移可用带宽
//            eval_end_time[i] = start_time[i] + eval_exec_time ; // 预估迁移任务结束时间 = 开始时间 + 预估执行时间

        }
        return start_time;
    }

}
