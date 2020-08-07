package org.cloudbus.cloudsim.examples.CBS.MigrationTest;

import org.cloudbus.cloudsim.examples.CBS.CBSRunner;

import java.io.IOException;

public class Lrrcos {
    public static void main(String[] args) throws IOException {
        boolean enableOutput = true;
        boolean outputToFile = true;
        String inputFolder = WithoutMigration.class.getClassLoader().getResource("workload.CBS").getPath();
        String outputFolder = "output";
        String workload = "20"; // CBS workload
        String vmAllocationPolicy = "CBS_lrr"; // First Fit without CD migrations
        String vmSelectionPolicy = "cos";
        String parameter = "1.2"; // the safety parameter of the LRR policy

        new CBSRunner(
                enableOutput,
                outputToFile,
                inputFolder,
                outputFolder,
                workload,
                vmAllocationPolicy,
                vmSelectionPolicy,
                parameter);
    }

}
