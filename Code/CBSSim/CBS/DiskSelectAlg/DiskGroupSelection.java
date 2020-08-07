package org.cloudbus.cloudsim.examples.CBS.DiskSelectAlg;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.entity.CloudDisk;
import org.cloudbus.cloudsim.entity.Warehouse;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;

import java.util.ArrayList;
import java.util.List;

public class DiskGroupSelection extends PowerVmSelectionPolicy {

    private List<Vm> priorVmList =  new ArrayList<Vm>();

    /**
     * 对云盘进行优先级排序，优先级靠前的优先被迁移
     * 定义metric为优先级指标，这里为与仓库负载的余弦相似度，容量高的仓库优先调度容量型负载
     */
    public void sortVmByPrior(PowerHost host){
        List<Vm> prior =  new ArrayList<>();
        List<Double> suit_metric = new ArrayList<>();
        List<PowerVm> migratableVms = getMigratableVms(host);
        if (migratableVms.isEmpty()) {
            return;
        }
        Warehouse wh = (Warehouse) host;
        double[] warehouse_res_vector = wh.getWarehouse_Res_Vector();
        for (Vm vm : migratableVms) {
            if (vm.isInMigration()) {
                continue;
            }
            CloudDisk cd = (CloudDisk) vm;
            double[] res_vector = cd.getAllDimUtilizationMean();
            double metric = res_vector[0] * warehouse_res_vector[0]
                    + res_vector[1] * warehouse_res_vector[1]
                    + res_vector[2] * warehouse_res_vector[2]
                    + res_vector[3] * warehouse_res_vector[3]
                    + res_vector[4] * warehouse_res_vector[4];
            if(suit_metric.size()==0){
                suit_metric.add(metric);
                prior.add(vm);
                continue;
            }
            for(int i=0;i<suit_metric.size();i++){
                if(metric>suit_metric.get(i)){
                    suit_metric.add(i,metric);
                    prior.add(i,vm);
                    break;
                }
                else if(i == suit_metric.size()-1){
                    suit_metric.add(metric);
                    prior.add(vm);
                    break;
                }
            }
        }
        priorVmList = prior;

    }

    @Override
    public Vm getVmToMigrate(PowerHost host) {
        Vm vmToMigrate = null;
        for (Vm vm : priorVmList) {
            if (vm.isInMigration()) {
                continue;
            }
            vmToMigrate = vm;
            break;
        }
        return vmToMigrate;
    }

}
