package org.cloudbus.cloudsim.examples.CBS.MigrationTest;

import org.cloudbus.cloudsim.examples.CBS.CBSRunner;

import java.io.IOException;

public class RunAllTest {
    public static void main(String[] args) throws IOException {
        boolean enableOutput = true;
        boolean outputToFile = true;
        String inputFolder = WithoutMigration.class.getClassLoader().getResource("workload2020").getPath();
        String outputFolder = "output";
        String workload = "Shanghai"; // CBS workload
        String[] vmAllocationPolicy = {"CBS_thr","CBS_mad","CBS_iqr","CBS_lr"}; // First Fit without CD migrations
        String[] vmSelectionPolicy = {"rs","mu","mmt","mc","cos"};
        String[] parameter = {"0.9","2.5","1.5","1.2"};
        for(int i=0;i<vmAllocationPolicy.length;i++){
            for(int j=0;j<vmSelectionPolicy.length;j++){
                new CBSRunner(
                        enableOutput,
                        outputToFile,
                        inputFolder,
                        outputFolder,
                        workload,
                        vmAllocationPolicy[i],
                        vmSelectionPolicy[j],
                        parameter[i]);
                }
            }

        }

}
