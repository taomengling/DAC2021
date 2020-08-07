package org.cloudbus.cloudsim.examples.CBS.CBSScheduleSystem;

import org.cloudbus.cloudsim.entity.CBS_Constants;
import org.cloudbus.cloudsim.examples.CBS.CBSRunner;

import java.io.IOException;

/**
 * A simulation of a heterogeneous power aware data center that applies the Static Threshold (THR)
 * VM allocation policy and Random Selection (RS) VM selection policy.
 * 
 * This example uses a real PlanetLab workload: 20110303.
 * 
 * The remaining configuration parameters are in the Constants and PlanetLabConstants classes.
 * 
 * If you are using any algorithms, policies or workload included in the power package please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since Jan 5, 2012
 */
public class CurrentScheduleSystem {

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(String[] args) throws IOException {
		boolean enableOutput = true;
		boolean outputToFile = false;
		//String inputFolder = WithoutMigration.class.getClassLoader().getResource("workload2020").getPath();
		String inputFolder = CBS_Constants.cbs_trace;
		String outputFolder = "output";
		String workload = "Shanghai"; // CBS workload
		String vmAllocationPolicy = "thr-based"; // Threhold-Based migration strategy
		String vmSelectionPolicy = "max-util"; // Max-Util-Based disk selection strategy
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
