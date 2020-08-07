package org.cloudbus.cloudsim.examples.CBS.DiskSelectAlg;

import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.entity.Warehouse;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerVm;
import org.cloudbus.cloudsim.power.PowerVmSelectionPolicy;

import java.util.List;



public class MaxUtilDiskSelection extends PowerVmSelectionPolicy {
        @Override
        public Vm getVmToMigrate(PowerHost host) {
            List<PowerVm> migratableVms = getMigratableVms(host);
            if (migratableVms.isEmpty()) {
                return null;
            }
            double[] weight = new double[5];
            Warehouse wh = (Warehouse) host;
            weight[0] = Math.abs((wh.getUtilizationCap())/wh.getRam());
            weight[1] = Math.abs((wh.getUtilizationWriteIops())/wh.getWrite_iops_limit());
            weight[2] = Math.abs((wh.getUtilizationWritebw())/wh.getWrite_bw_limit());

            Vm vmToMigrate = null;
            double maxMetric = Double.MIN_VALUE;
            for (Vm vm : migratableVms) {
                if (vm.isInMigration()) {
                    continue;
                }
                double cap = (double)vm.getCurrentAllocatedCap()/vm.getRam();
                double write_iops = vm.getCurrentAllocatedWrite_IOPS() /vm.getMips();
                double write_bw = (double)vm.getCurrentAllocatedWrite_Bw()/vm.getBw();
//                double metric = cap * weight[0] + write_iops * weight[1] + write_bw * weight[2];
                double metric = cap + write_iops + write_bw;
                if (metric > maxMetric) {
                    maxMetric = metric;
                    vmToMigrate = vm;
                }
            }
            return vmToMigrate;
        }

    }
