package org.cloudbus.cloudsim.entity;

import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G4Xeon3040;
import org.cloudbus.cloudsim.power.models.PowerModelSpecPowerHpProLiantMl110G5Xeon3075;

/**
 * If you are using any algorithms, policies or workload included in the power package, please cite
 * the following paper:
 *
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 *
 * @author Anton Beloglazov
 * @since Jan 6, 2012
 */
public class CBS_Constants {
	public final static String cbs_trace = "E://cloudsim4/cloudsim-cloudsim-4.0/modules/cloudsim-examples/src/main/resources/workload2020";
	public final static String host_state_output_dir = "E:/cloudsim4/cloudsim-cloudsim-4.0/output/host_state/";
	public final static boolean ENABLE_OUTPUT = true;
	public final static boolean OUTPUT_CSV    = false;

	public final static double SCHEDULING_INTERVAL =300;
	public final static double SIMULATION_LIMIT = 24 * 60 * 60 * 4;

	public final static  int CLOUDLET_LENGTH	=  Integer.MAX_VALUE;
	public final static int CLOUDLET_PES	= 1;

	/*
	 * VM instance types:
	 *   High-Memory Extra Large Instance: 3.25 EC2 Compute Units, 8.55 GB // too much MIPS
	 *   High-CPU Medium Instance: 2.5 EC2 Compute Units, 0.85 GB
	 *   Extra Large Instance: 2 EC2 Compute Units, 3.75 GB
	 *   Small Instance: 1 EC2 Compute Unit, 1.7 GB
	 *   Micro Instance: 0.5 EC2 Compute Unit, 0.633 GB
	 *   We decrease the memory size two times to enable oversubscription
	 *
	 */
//	public final static int VM_TYPES	= 4;
//	public final static int[] VM_MIPS	= { 2500, 2000, 1000, 500 };
//	public final static int[] VM_PES	= { 1, 1, 1, 1 };
//	public final static int[] VM_RAM	= { 870,  1740, 1740, 613 };
//	public final static int VM_BW		= 100000; // 100 Mbit/s
//	public final static int VM_SIZE		= 2500; // 2.5 GB
	//模拟CBS云盘类型
	public final static int   VM_all_Resource_limit	= 100; //为了保持原有框架 无作用 设定任意值都可
	public final static int   VM_TYPES	= 2;//官网上的销售类型 高性能云硬盘和SSD云硬盘
	public final static int[] VM_MIPS	= { VM_all_Resource_limit, VM_all_Resource_limit};//单盘最大IOPS
	public final static int[] VM_PES	= { 1, 1 };//模拟云盘的虚拟机核数
	public final static int[] VM_RAM	= { VM_all_Resource_limit,VM_all_Resource_limit };//模拟云盘的容量 实际容量大小从数据集中读取
	public final static int[] VM_BW		= {VM_all_Resource_limit,VM_all_Resource_limit}; // 模拟云盘的带宽 高性能:150MB/s SSD：260Mb/s 设定为相同值即可
	public final static int VM_SIZE		= 0; // 2.5 GB //暂无作用

	/*
	 * Host types:
	 *   HP ProLiant ML110 G4 (1 x [Xeon 3040 1860 MHz, 2 cores], 4GB)
	 *   HP ProLiant ML110 G5 (1 x [Xeon 3075 2660 MHz, 2 cores], 4GB)
	 *   We increase the memory size to enable over-subscription (x4)
	 */
//	public final static int HOST_TYPES	 = 2;
//	public final static int[] HOST_MIPS	 = { 1860*2, 2660*2 };
//	public final static int[] HOST_PES	 = { 1, 1 };
//	public final static int[] HOST_RAM	 = { 4096, 4096 };
//	public final static int HOST_BW		 = 1000000; // 1 Gbit/s
//	public final static int HOST_STORAGE = 1000000; // 1 GB
	//用HOST模拟CBS仓库 两种：FCBS HCBS
	public final static int HOST_TYPES	 = 2;
	public final static int[] HOST_MIPS	 = {14438 ,  320000};//FCBS HCBS 的写IOPS上限
	public final static int[] HOST_PES	 = { 1, 1 }; //模拟仓库的物理机核数
	public final static int[] HOST_RAM	 = { 0, 400000 };//仓库的容量大小 实际值从数据集中读取
	//public final static int[] HOST_BW	 = {1493101,4142663}; // 仓库写带宽上线 单位KB/S
	public final static int[] HOST_BW	 = {Integer.MAX_VALUE,5452592}; // 仓库写带宽上线 单位KB/S
	public final static int HOST_STORAGE = 1000000 ; // 1 GB



	public final static int[] Warehouse_device_num	 = { 1, 1 }; //模拟仓库的物理机核数
	public final static int[] Warehouse_storage_size	 = { 0, 400000 };//仓库的容量大小 实际值从数据集中读取
	public final static int[] Warehouse_Read_IOPS_Limit	 = {0,320000}; // 仓库读IOPS上线 单位 次/S
	public final static int[] Warehouse_Write_IOPS_Limit  = {14438 ,  3200000};//FCBS HCBS 的写IOPS上限
	public final static int[] Warehouse_Read_BW_Limit	 = {0,5452592}; // 仓库读带宽上线 单位 KB/S
	public final static int[] Warehouse_Write_BW_Limit	 = {Integer.MAX_VALUE,5452592}; // 仓库写带宽上线 单位KB/S

	public final static int migration_ctrl = 3; //迁移可用写总带宽 单位:MB
	public final static int migration_bw = 100; //单盘最大迁移带宽速度 单位:MB


	public final static double OverbookingRate = 0;
	public final static PowerModel[] HOST_POWER = {//能耗模型
		new PowerModelSpecPowerHpProLiantMl110G4Xeon3040(),
		new PowerModelSpecPowerHpProLiantMl110G5Xeon3075()
	};

}
