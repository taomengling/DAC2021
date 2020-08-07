package org.cloudbus.cloudsim.examples.CBS.MigrationTest;

import org.cloudbus.cloudsim.examples.CBS.CBSRunner;

import java.io.IOException;

public class ThrCos {

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void main(String[] args) throws IOException {
        boolean enableOutput = true;
        boolean outputToFile = true;
        String inputFolder = WithoutMigration.class.getClassLoader().getResource("workload.CBS").getPath();
        String outputFolder = "output";
        String workload = "20"; // CBS workload
        String vmAllocationPolicy = "CBS_thr"; // First Fit without CD migrations
        String vmSelectionPolicy = "cos";
        String parameter = "0.9";

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
