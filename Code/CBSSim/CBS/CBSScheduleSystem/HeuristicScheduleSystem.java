package org.cloudbus.cloudsim.examples.CBS.CBSScheduleSystem;

import org.cloudbus.cloudsim.entity.CBS_Constants;
import org.cloudbus.cloudsim.examples.CBS.CBSRunner;

import java.io.IOException;

public class HeuristicScheduleSystem {


    public static void main(String[] args) throws IOException {
        boolean enableOutput = true;
        boolean outputToFile = false;
        String inputFolder = CBS_Constants.cbs_trace;
        String outputFolder = "output";
        String workload = "Shanghai"; // CBS workload
        String vmAllocationPolicy = "heuristic-based"; // heuristic migration strategy
        String vmSelectionPolicy = "heuristic-based"; // heuristic disk selection strategy
        String balance_parameter = "0.05"; // 均衡度阈值

        new CBSRunner(
                enableOutput,
                outputToFile,
                inputFolder,
                outputFolder,
                workload,
                vmAllocationPolicy,
                vmSelectionPolicy,
                balance_parameter);
    }
}
