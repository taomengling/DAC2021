package org.cloudbus.cloudsim.entity;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.power.*;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.util.MathUtil;

import java.io.File;
import java.io.*;
import java.util.*;

/**
 * A helper class for the running examples for the PlanetLab workload.
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
public class CBSHelper  {

	private static Map<String,Double> Disk_feature=new HashMap<String, Double>();
	private static Map<String,int[]> Warehouse_feature= new HashMap<String,int[]>();
	/**
	 * Creates the cloudlet list planet lab.
	 * 
	 * @param brokerId the broker id
	 * @param inputFolderName the input folder name
	 * @return the list
	 * @throws FileNotFoundException the file not found exception
	 */
	/**
	 * 创建CBS 多个维度的资源利用率数组
	 * @param inputPath
	 * @return 资源利用率数组
	 */
	public static double[][] create_UtilCBS_trace_list(String inputPath)
			throws NumberFormatException,
			IOException {
		Log.printLine("Reading trace data...");
		ArrayList<double[]> trace_list = new ArrayList<>();

		int length = (int) (CBS_Constants.SIMULATION_LIMIT/CBS_Constants.SCHEDULING_INTERVAL)+1;
		double[][] data = new double[4][length];//trace data
		BufferedReader input = new BufferedReader(new FileReader(inputPath));
		String tmpvalue = input.readLine();
		int n = length;
		for (int i = 0; i < n - 1; i++) {
			tmpvalue = input.readLine();
			if (tmpvalue == null) {
				break;
			}
			String[] valuelist = tmpvalue.split(",");
			//#得到工作负载数组，0：读iops 1：写iops 2：读带宽 3：写带宽
			double thr = 100;
			for(int j=0;j<4;j++){
				double workload = Integer.parseInt(valuelist[j + 1]);
				double used_resource = (workload) / (CBS_Constants.VM_all_Resource_limit);
				if (used_resource < 0) {
					used_resource = 0;
				}
				data[j][i] = used_resource;
			}
		}
		data[0][n - 1] = data[0][n - 2];
		data[1][n - 1] = data[1][n - 2];
		data[2][n - 1] = data[2][n - 2];
		data[3][n - 1] = data[3][n - 2];
		input.close();
		return  data;

	}
	public static List<Cloudlet> createCloudletListPlanetLab(int brokerId, String inputFolderName)
			throws IOException {
		List<Cloudlet> list = new ArrayList<Cloudlet>();
		//ReadWarehouseFeature();
		long fileSize = 300;
		long outputSize = 300;
		UtilizationModel utilizationModelNull = new UtilizationModelFull();
		//利用率模型 空利用率

		File inputFolder = new File(inputFolderName);
		String[] buss_file = inputFolder.list();
		int now_disk_num=0;
		for (int buss_num = 0; buss_num < buss_file.length; buss_num++) {
			//建立有订阅信息的云盘hash表
			String[] buss_name = (buss_file[buss_num]).split("_");
			String disk_info_file = inputFolderName + "/" + buss_file[buss_num] + "/" + buss_name[1] + "_subscript_info";
			BufferedReader disk_input = new BufferedReader(new FileReader(disk_info_file));
			Map<String,Integer> disk_id2size = new HashMap<String, Integer>();
			while (true){
				String tmp_value = disk_input.readLine();
				if (tmp_value == null){
					break;
				}
				String[] feature = tmp_value.split(",");
				int total_size = Integer.parseInt(feature[feature.length-1]);
				String disk_id = feature[1];
				disk_id2size.put(disk_id,total_size);
			}
			//建立有订阅信息的云盘hash表

			String bussFolderName=inputFolderName+"/"+buss_file[buss_num];
			File bussFolder=new File(bussFolderName);
			//读取该仓库中云盘特征
			//ReadDiskFeature(clouddiskinfoFolder);
			now_disk_num =list.size();
			File[] files = bussFolder.listFiles();//遍历每个云盘的负载
			//创建负载中的云任务
			for (int i = 0; i < files.length; i++) {
//			for (int i = 0; i < 2000; i++) {
				String disk_id = files[i].getName();
				if (disk_id.length() != 36) {
					continue;
				}
				if (disk_id2size.get(disk_id)==null){
					continue;
				}
				Cloudlet cloudlet = null;
				try {
					double[][] disk_trace = create_UtilCBS_trace_list(files[i].getAbsolutePath());//0:读iopstrace 1:写iopstrace 2:读带宽 3:写带宽
					cloudlet = new CD_Workload(
							i+now_disk_num,
							CBS_Constants.CLOUDLET_LENGTH,
							CBS_Constants.CLOUDLET_PES,
							fileSize,
							outputSize,
							new UtilizationModelCBS(
									CBS_Constants.SCHEDULING_INTERVAL,disk_trace[1]),
							utilizationModelNull,
							new UtilizationModelCBS(
									CBS_Constants.SCHEDULING_INTERVAL,disk_trace[3]),
							new UtilizationModelCBS(
									CBS_Constants.SCHEDULING_INTERVAL,disk_trace[0]),
							new UtilizationModelCBS(
									CBS_Constants.SCHEDULING_INTERVAL,disk_trace[2]));
//					cloudlet = new CD_Workload(
//							i+now_disk_num,
//							CBS_Constants.CLOUDLET_LENGTH,
//							CBS_Constants.CLOUDLET_PES,
//							fileSize,
//							outputSize,
//							new UtilizationModelCBS(
//									files[i].getAbsolutePath(),
//									CBS_Constants.SCHEDULING_INTERVAL,"write_iops"),
//							utilizationModelNull,
//							new UtilizationModelCBS(
//									files[i].getAbsolutePath(),
//									CBS_Constants.SCHEDULING_INTERVAL,"write_bw"),
//							new UtilizationModelCBS(
//									files[i].getAbsolutePath(),
//									CBS_Constants.SCHEDULING_INTERVAL,"read_iops"),
//							new UtilizationModelCBS(
//								files[i].getAbsolutePath(),
//								CBS_Constants.SCHEDULING_INTERVAL,"read_bw"));
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
				cloudlet.setUserId(brokerId);
				cloudlet.setVmId(list.size());
				list.add(cloudlet);
			}
		}

		return list;//云任务列表，负载为MIPS利用率
	}

	public static void ReadDiskFeature(String clouddiskinfoFolder)
			throws NumberFormatException,
			IOException {
		BufferedReader input = new BufferedReader(new FileReader(clouddiskinfoFolder));
		String tmpvalue="";
		while((tmpvalue=input.readLine())!=null){
			String[] valuearray=tmpvalue.split(",");
			Double cap=Double.valueOf(valuearray[7]);
			Disk_feature.put(valuearray[1]+"cap",cap);
		}
	}

	public static void ReadWarehouseFeature()
			throws NumberFormatException,
			IOException {
		String WareHouseinfoFolder="E:/CloudSim/cloudsim-cloudsim-4.0/modules/cloudsim-examples/src/main/resources/workload/CBS/warehouseInfo.csv";
		BufferedReader input = new BufferedReader(new FileReader(WareHouseinfoFolder));
		String tmpvalue=input.readLine();
		while((tmpvalue=input.readLine())!=null){
			String[] valuearray=tmpvalue.split(",");
			int type=1;
			if ((valuearray[8].equals("FCBS"))){type=0;}
			int cap=Integer.parseInt(valuearray[4]);
			int[] feature={type,cap};
			Warehouse_feature.put(valuearray[0]+"_"+valuearray[1],feature);
		}
	}
	/**
	 * Creates the vm list.
	 *
	 * @param brokerId the broker id
	 *# @param vmsNumber the vms number
	 *
	 * @return the list< vm>
	 */
	public static List<Vm> createVmList(int brokerId, List<Cloudlet> cloudletList, String inputFolderName)
			throws NumberFormatException,
			IOException{
		List<Vm> vms = new ArrayList<Vm>();
		int CD_id=0;
		File inputFolder = new File(inputFolderName);
		String[] buss_file = inputFolder.list();
		for (int buss_num = 0; buss_num < buss_file.length; buss_num++) {
			String bussFolderName=inputFolderName+"/"+buss_file[buss_num];//仓库名路径
			String[] buss_name = (buss_file[buss_num]).split("_");
			String disk_info_file = inputFolderName + "/" + buss_file[buss_num] + "/" + buss_name[1] + "_subscript_info";
			BufferedReader disk_input = new BufferedReader(new FileReader(disk_info_file));
			Map<String,Integer> disk_id2size = new HashMap<String, Integer>();
			while (true){
				String tmp_value = disk_input.readLine();
				if (tmp_value == null){
					break;
				}
				String[] feature = tmp_value.split(",");
				int total_size = Integer.parseInt(feature[feature.length-1]);
				String disk_id = feature[1];
				disk_id2size.put(disk_id,total_size);
			}

			File bussFolder=new File(bussFolderName);
			File[] files = bussFolder.listFiles();//遍历每个云盘的负载
			for (int i = 0; i < files.length; i++) {
				if (CD_id>=cloudletList.size()){ //云盘ID和任务ID 一 一 对应
					break;
				}
				String disk_id = files[i].getName();
				if (disk_id.length() != 36) {// 跳过非trace文件
					continue;
				}
				int vmType=1;//SSD云盘
				String[] disk_info=disk_id.split("_");
				int disk_cap=0;
				if (disk_id2size.get(disk_id) == null) {
					//删除对应云IO负载
//					cloudletList.remove(CD_id);
//					CD_id++;
					disk_cap = 50;
//					continue;
				}else {
					disk_cap = disk_id2size.get(disk_id);
				}
				vms.add(new CloudDisk(
						CD_id,
						brokerId,
						CBS_Constants.VM_MIPS[vmType],
						CBS_Constants.VM_PES[vmType],
						((int) disk_cap),
						CBS_Constants.VM_BW[vmType],
						CBS_Constants.VM_SIZE,
						1,
						"Xen",
						new CD_CloudletSchedulerDynamicWorkload(CBS_Constants.VM_MIPS[vmType], CBS_Constants.VM_PES[vmType]),
						CBS_Constants.SCHEDULING_INTERVAL,
						buss_num));
				CD_id++;
			}
			//为每个仓库添加一个云盘，这个云盘不占用物理资源，只在迁移时分配物理资源，模拟迁移占用的带宽资源
//			CloudDisk virtual_migration_disk = new CloudDisk()

		}
		return vms;
	}

	/**
	 * Creates the host list.
	 *
	 *# @param hostsNumber the hosts number
	 *
	 * @return the list< power host>
	 */
	public static List<PowerHost> createHostList(String inputFolderName) {
		List<PowerHost> hostList = new ArrayList<PowerHost>();
		File inputFolder = new File(inputFolderName);
		String[] buss_file = inputFolder.list();
		int[] buss_feature={0,0};
		for (int i = 0; i < buss_file.length; i++) {
			String buss_name=buss_file[i];
			int hostType = 1;
			int buss_cap= CBS_Constants.Warehouse_storage_size[hostType];
			List<Pe> peList = new ArrayList<Pe>();
			for (int j = 0; j < CBS_Constants.Warehouse_device_num[hostType]; j++) {
				peList.add(new Pe(j, new PeProvisionerSimple(CBS_Constants.HOST_MIPS[hostType])));
			}

			hostList.add(new Warehouse(
					i,
					new RamProvisionerSimple(buss_cap),
					new BwProvisionerSimple(CBS_Constants.HOST_BW[hostType]),
					CBS_Constants.HOST_STORAGE,
					peList,
					new VmSchedulerTimeSharedOverSubscription(peList),
					CBS_Constants.HOST_POWER[hostType],
					CBS_Constants.Warehouse_Write_IOPS_Limit[hostType],
					CBS_Constants.Warehouse_Write_BW_Limit[hostType],
					CBS_Constants.Warehouse_Read_IOPS_Limit[hostType],
					CBS_Constants.Warehouse_Read_BW_Limit[hostType]));
		}
		return hostList;
	}

	/**
	 * Creates the broker.
	 *
	 * @return the datacenter broker
	 */
	public static DatacenterBroker createBroker() {
		DatacenterBroker broker = null;
		try {
			broker = new PowerDatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return broker;
	}

	/**
	 * Creates the datacenter.
	 *
	 * @param name the name
	 * @param datacenterClass the datacenter class
	 * @param hostList the host list
	 * @param vmAllocationPolicy the vm allocation policy
	 * @return the power datacenter
	 *
	 * @throws Exception the exception
	 */
	public static Datacenter createDatacenter(
			String name,
			Class<? extends Datacenter> datacenterClass,
			List<PowerHost> hostList,
			VmAllocationPolicy vmAllocationPolicy) throws Exception {
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this resource
		double costPerBw = 0.0; // the cost of using bw in this resource

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch,
				os,
				vmm,
				hostList,
				time_zone,
				cost,
				costPerMem,
				costPerStorage,
				costPerBw);

		Datacenter datacenter = null;
		try {
			datacenter = datacenterClass.getConstructor(
					String.class,
					DatacenterCharacteristics.class,
					VmAllocationPolicy.class,
					List.class,
					Double.TYPE).newInstance(
					name,
					characteristics,
					vmAllocationPolicy,
					new LinkedList<Storage>(),
					CBS_Constants.SCHEDULING_INTERVAL);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		return datacenter;
	}

	/**
	 * 保存实验时间内仓库5min状态
	 */
	public static void saveHostState(List<Host> hostlist)
			throws IOException{
		String out_dir = CBS_Constants.host_state_output_dir;
		for(int i =0;i<hostlist.size();i++) {
			File file = new File(out_dir+"host_state_" + (i)+ ".txt");
			FileWriter fileWritter = new FileWriter(file.getPath(), false);
			fileWritter.write("read_iops,");
			fileWritter.write("write_iops,");
			fileWritter.write("read_bandwidth,");
			fileWritter.write("write_bandwidth,");
			fileWritter.write("cap");
			fileWritter.write('\n');
			PowerHostUtilizationHistory host = (PowerHostUtilizationHistory) hostlist.get(i);
			List<HostStateHistoryEntry> history = host.getStateHistory();
			for (int j = 0; j < history.size(); j++) {
				HostStateHistoryEntry state = history.get(j);
				String record = String.valueOf(state.getRequestedReadIOPS()) + ','+
							String.valueOf(state.getRequestedWriteIOPS()) + ','+
						    String.valueOf(state.getRequestedReadBandwidth())+ ','+
							String.valueOf(state.getRequestedWriteBandwidth())+ ','+
							String.valueOf(state.getRequestedCap())+'\n';
				fileWritter.write(record);
			}
			fileWritter.close();
		}
	}

	/**
	 * 得到迁移过程中，用户写IO流量的数量，描述双写的额外流量和与用户IO的争用程度
	 * @param vms 系统中所有的云盘列表
	 * @return 额外流量
	 */
	public static int getThroughout_WhileMigration(List<Vm> vms){
		int traffic = 0;
		for (Vm vm : vms) {
			if (vm.getBeMigrationTime() == 0) {
				continue; // 没有迁移过的云盘跳过检查
			}
			List<VmStateHistoryEntry> stateHistory = vm.getStateHistory();
			for (VmStateHistoryEntry state : stateHistory) {// 检查历史记录
				if (state.isInMigration()) {
					traffic += state.getBw();
				}

			}
		}
		return  traffic/1000;
	}


	/**
	 * 得到迁移过程中，出现过载现象的时间，表示迁移任务分配不合理，导致迁移流量进一步加重过载
	 * @param vms 系统中所有的云盘列表
	 * @return 额外流量
	 */
	public static double getOverloadTime_WhileMigration(List<Vm> vms){
		double time = 0;
		for (Vm vm : vms) {
			if (vm.getBeMigrationTime() == 0) {
				continue; // 没有迁移过的云盘跳过检查
			}
			List<VmStateHistoryEntry> stateHistory = vm.getStateHistory();
			for (VmStateHistoryEntry state : stateHistory) {// 检查历史记录
				if (state.isInMigration() && state.isHostOverLoad()) {
					time += CBS_Constants.SCHEDULING_INTERVAL;
				}

			}
		}
		return  time;
	}

	/**
	 * 得到整个迁移过程中，被重复迁移的云盘次数，这个指标代表迁移过程的收敛性，如果迁移目标选择不当，可能会导致目的仓库重新出现不均衡，又重复迁出该云盘
	 *
	 * @param vms 系统中所有的云盘列表
	 * @return 出现迁移次数大于1的云盘个数
	 */
	public static int get_extra_migration_vm(List<Vm> vms){
		int vm_count = 0;
		for (Vm vm : vms) {
				if (vm.getBeMigrationTime()>1) {
					vm_count += (vm.getBeMigrationTime()-1);
				}
		}
		return  vm_count;
	}

	/**
	 * 得到整个迁移过程中,盘的平均迁移耗时
	 *
	 * @param vms 系统中的的云盘，模拟过程总迁移次数
	 * @return 出现
	 */
	public static double getAverMigrationTime(List<Vm> vms,int migration_times){
		if(migration_times==0){
			return 0;
		}
		double average_time = 0;
		for (Vm vm : vms) {
			double vm_migration_time = 0;
			if (vm.getBeMigrationTime() == 0) {
				continue; // 没有迁移过的云盘跳过检查
			}
			List<VmStateHistoryEntry> stateHistory = vm.getStateHistory();
			for (VmStateHistoryEntry state : stateHistory) {// 检查历史记录
				if (state.isInMigration()) {
					vm_migration_time += CBS_Constants.SCHEDULING_INTERVAL;
				}
			}
			average_time += vm_migration_time;
		}
		average_time = average_time/migration_times;
		average_time = average_time/60; // 转化为分钟
		return  average_time;
	}


	/**
	 * 获得存储集群整体的不均衡度：
	 * 不均衡度定义 所有维度资源利用率间的方差和，方差越大，表示仓库间的资源利用率相差越大，资源不均衡程度越高。(方差中就包含了资源利用率越高，
	 * 相同离散程度的维度方差值越高，符合衡量的定义)
	 * @param hosts 当前系统中的所有仓库
	 * @return 不均衡度
	 */

	public static double getLoadBalance(List<Host> hosts){
		double balance = 0;
		double[] balance_all_dim =  new double[5];
		double[][] warehouse_load = new double[hosts.size()][5];
		double[][] selected_wh_array = new double[1][hosts.size()];
		for(int i = 0; i< hosts.size();i++){
			selected_wh_array[0][i] = 1;
		}
		for(int i=0;i< hosts.size();i++){
			Warehouse wh = (Warehouse)hosts.get(i);
			warehouse_load[i] = wh.getWarehouse_Res_Vector();
		}
		RealMatrix load_matrix = new Array2DRowRealMatrix(warehouse_load);
		RealMatrix selected_wh_matrix = new Array2DRowRealMatrix(selected_wh_array);
		RealMatrix system_av_load = selected_wh_matrix.multiply(load_matrix); //[]
		for(int i=0;i<5;i++){
			system_av_load.setEntry(0,i,system_av_load.getEntry(0,i)/hosts.size());
		}
		for(int i=0;i<hosts.size();i++) {
			load_matrix.setRowMatrix(i,load_matrix.getRowMatrix(i).subtract(system_av_load));
			balance_all_dim[0] += Math.abs(load_matrix.getEntry(i,0));
			balance_all_dim[1] += Math.abs(load_matrix.getEntry(i,1));
			balance_all_dim[2] += Math.abs(load_matrix.getEntry(i,2));;
			balance_all_dim[3] += Math.abs(load_matrix.getEntry(i,3));
			balance_all_dim[4] += Math.abs(load_matrix.getEntry(i,4));
		}
		balance = balance_all_dim[0]+balance_all_dim[1]+balance_all_dim[2]+balance_all_dim[3]+balance_all_dim[4];
		return balance/hosts.size();

	}
	public static void printResults(
			PowerDatacenter datacenter,
			List<Vm> vms,
			double lastClock,
			String experimentName,
			boolean outputInCsv,
			String outputFolder) {
		Log.setDisabled(false);
		Log.enable();
		List<Host> hosts = datacenter.getHostList();
		try {
			saveHostState(hosts);
		} catch (IOException e) {
			System.out.println("create file failed! ");
			e.printStackTrace();
		}
		int numberOfHosts = hosts.size();
		int numberOfVms = vms.size();

		double totalSimulationTime = lastClock;
		int numberOfMigrations = datacenter.getMigrationCount();
		int sizeOfMigrations = datacenter.getMigrationSize();
		double average_migration_time_per_disk = getAverMigrationTime(vms,numberOfMigrations);
		double energy = 0.0;

		double slaTimePerActiveHost = getSlaTimePerActiveHost(hosts);
		double slaperfdegradation = getSlaDegradation(hosts);
		int traffic_whileMigration = getThroughout_WhileMigration(vms); // 迁移期间用户写IO总量,表示双写迁移的额外写入数据量
		double overload_time_whileMigration = getOverloadTime_WhileMigration(vms);
		double balance_metric = getLoadBalance(hosts);
		double sla = slaTimePerActiveHost *  slaperfdegradation;
		int extra_migration_vm_count = get_extra_migration_vm(vms);
		Log.setDisabled(false);
		Log.printLine();
		Log.printLine(String.format("Experiment name: " + experimentName));
		Log.printLine(String.format("Number of Warehouses: " + numberOfHosts));
		Log.printLine(String.format("Number of Cloud_disks: " + numberOfVms));
		Log.printLine(String.format("Total simulation time: %.2f sec", totalSimulationTime));
		Log.printLine(String.format("Average life last: %.2f kWh", energy));
		Log.printLine(String.format("Times of disk migrations: %d times", numberOfMigrations));
		Log.printLine(String.format("Cost of disk migrations: %d GB", sizeOfMigrations));
		Log.printLine(String.format("Average Migration time of disk : %.2f min", average_migration_time_per_disk));
		Log.printLine(String.format("Cost of double write : %d MB", traffic_whileMigration));
		Log.printLine(String.format("Number of vm extra migration : %d times", extra_migration_vm_count));
		Log.printLine(String.format("load balance of total system : %.10f", balance_metric));
		Log.printLine(String.format("SLA violation caused by migration : %f ", overload_time_whileMigration));
		Log.printLine(String.format("SLA: %.5f%%", sla * 100));
		Log.printLine(String.format("SLA perf degradation : %.5f%%", slaperfdegradation * 100));
		Log.printLine(String.format("SLA time per active host: %.5f%%", slaTimePerActiveHost * 100));;

		if (datacenter.getVmAllocationPolicy() instanceof PowerVmAllocationPolicyMigrationAbstract) {
				PowerVmAllocationPolicyMigrationAbstract vmAllocationPolicy = (PowerVmAllocationPolicyMigrationAbstract) datacenter
						.getVmAllocationPolicy();

				double executionTimeVmSelectionMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryVmSelection());
				double executionTimeVmSelectionStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryVmSelection());
				double executionTimeHostSelectionMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryHostSelection());
				double executionTimeHostSelectionStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryHostSelection());
				double executionTimeVmReallocationMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryVmReallocation());
				double executionTimeVmReallocationStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryVmReallocation());
				double executionTimeTotalMean = MathUtil.mean(vmAllocationPolicy
						.getExecutionTimeHistoryTotal());
				double executionTimeTotalStDev = MathUtil.stDev(vmAllocationPolicy
						.getExecutionTimeHistoryTotal());

				Log.printLine(String.format(
						"Execution time - VM selection mean: %.5f sec",
						executionTimeVmSelectionMean));
				Log.printLine(String.format(
						"Execution time - VM selection stDev: %.5f sec",
						executionTimeVmSelectionStDev));
				Log.printLine(String.format(
						"Execution time - host selection mean: %.5f sec",
						executionTimeHostSelectionMean));
				Log.printLine(String.format(
						"Execution time - host selection stDev: %.5f sec",
						executionTimeHostSelectionStDev));
				Log.printLine(String.format(
						"Execution time - VM reallocation mean: %.5f sec",
						executionTimeVmReallocationMean));
				Log.printLine(String.format(
						"Execution time - VM reallocation stDev: %.5f sec",
						executionTimeVmReallocationStDev));
				Log.printLine(String.format("Execution time - total mean: %.5f sec", executionTimeTotalMean));
				Log.printLine(String
						.format("Execution time - total stDev: %.5f sec", executionTimeTotalStDev));
			}
			Log.printLine();
		Log.setDisabled(true);
	}
//	public static String parseExperimentName(String name) {
//		Scanner scanner = new Scanner(name);
//		StringBuilder csvName = new StringBuilder();
//		scanner.useDelimiter("_");
//		for (int i = 0; i < 4; i++) {
//			if (scanner.hasNext()) {
//				csvName.append(scanner.next() + ",");
//			} else {
//				csvName.append(",");
//			}
//		}
//		scanner.close();
//		return csvName.toString();
//	}
//	public static void writeDataRow(String data, String outputPath) {
//		File file = new File(outputPath);
//		try {
//			file.createNewFile();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//			System.exit(0);
//		}
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//			writer.write(data);
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(0);
//		}
//	}
//	public static void writeDataColumn(List<? extends Number> data, String outputPath) {
//		File file = new File(outputPath);
//		try {
//			file.createNewFile();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//			System.exit(0);
//		}
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//			for (Number value : data) {
//				writer.write(value.toString() + "\n");
//			}
//			writer.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//			System.exit(0);
//		}
//	}
//	public static void writeMetricHistory(
//			List<? extends Host> hosts,
//			PowerVmAllocationPolicyMigrationAbstract vmAllocationPolicy,
//			String outputPath) {
//		// for (Host host : hosts) {
//		for (int j = 0; j < 10; j++) {
//			Host host = hosts.get(j);
//
//			if (!vmAllocationPolicy.getTimeHistory().containsKey(host.getId())) {
//				continue;
//			}
//			File file = new File(outputPath + "_" + host.getId() + ".csv");
//			try {
//				file.createNewFile();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//				System.exit(0);
//			}
//			try {
//				BufferedWriter writer = new BufferedWriter(new FileWriter(file));
//				List<Double> timeData = vmAllocationPolicy.getTimeHistory().get(host.getId());
//				List<Double> utilizationData = vmAllocationPolicy.getUtilizationHistory().get(host.getId());
//				List<Double> metricData = vmAllocationPolicy.getMetricHistory().get(host.getId());
//
//				for (int i = 0; i < timeData.size(); i++) {
//					writer.write(String.format(
//							"%.2f,%.2f,%.2f\n",
//							timeData.get(i),
//							utilizationData.get(i),
//							metricData.get(i)));
//				}
//				writer.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//				System.exit(0);
//			}
//		}
//	}

	protected static double getSlaDegradation(List<Host> hosts) {
		double totalDegree = 0;
		int slaViolationTimes=0;

		for (Host _host : hosts) {
			Warehouse host = (Warehouse) _host;
			double previousTime = -1;
			boolean previousIsActive = true;

			for (HostStateHistoryEntry entry : host.getStateHistory()) {
				if (previousTime != -1 && previousIsActive) {
					double timeDiff = entry.getTime() - previousTime;
					if (entry.getRequestedWriteIOPS() > host.getWrite_iops_limit() ||
							entry.getRequestedBandwidth() > host.getWrite_bw_limit()) {
						double IOPS_violation = (entry.getRequestedWriteIOPS()-host.getWrite_iops_limit())/host.getWrite_iops_limit();
						IOPS_violation = Math.max(IOPS_violation,0);
						double BW_violation = (entry.getRequestedBandwidth()-host.getWrite_bw_limit())/host.getWrite_bw_limit();
						BW_violation = Math.max(BW_violation,0);
						double total_violation = IOPS_violation + BW_violation;
						totalDegree += total_violation;
						slaViolationTimes++;
					}
				}
				previousTime = entry.getTime();
				previousIsActive = entry.isActive();
			}
		}

		return totalDegree / slaViolationTimes;
		//return slaViolationTimes;
	}
	protected static double getSlaTimePerActiveHost(List<Host> hosts) {
		double slaViolationTimePerHost = 0;
		double totalTime = 0;
		int slaViolationTimes=0;

		for (Host _host : hosts) {
			Warehouse host = (Warehouse) _host;
			double previousTime = -1;
			boolean previousIsActive = true;

			for (HostStateHistoryEntry entry : host.getStateHistory()) {
				if (previousTime != -1 && previousIsActive) {
					double timeDiff = entry.getTime() - previousTime;
					totalTime += timeDiff;
					//检测是否过载  业务正常流量加上迁移流量
					if (entry.getRequestedWriteIOPS()  + entry.getMigration_in_num() * CBS_Constants.migration_bw > host.getWrite_iops_limit() ||
						entry.getRequestedWriteBandwidth() + entry.getMigration_in_num() * CBS_Constants.migration_bw * 1000 > host.getWrite_bw_limit()) {
						slaViolationTimePerHost += timeDiff;
						slaViolationTimes++;
					}
				}
				previousTime = entry.getTime();
				previousIsActive = entry.isActive();
			}
		}

		return slaViolationTimePerHost / totalTime;
		//return slaViolationTimes;
	}

}
