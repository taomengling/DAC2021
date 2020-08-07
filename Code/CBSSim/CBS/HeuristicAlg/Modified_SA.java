package org.cloudbus.cloudsim.examples.CBS.HeuristicAlg;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.entity.CloudDisk;
import org.cloudbus.cloudsim.entity.Warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 多目标模拟退火算法
 */
public class Modified_SA {
    public final double balance_qos_contraint = 0.05;

    List<CloudDisk> diskList = new ArrayList<>();
    List<Warehouse> warehouseList = new ArrayList<>();
    List<Map<String, Object>> Disk2WarehouseMap = new ArrayList<>();
    int[] pre_map_array;
    int iteration_limit = 10;
    int T_desc = 50; //降温次数
    double a = 0.98; //降温系数
    double init_temp = 100; //初始温度
    double[][] warehouse_util_state ;
    double[][] disk_util_vector;

    public Modified_SA(List<CloudDisk> diskList,List<Warehouse> warehouseList) {
        this.diskList = diskList;
        this.warehouseList = warehouseList;
        this.pre_map_array = init_pre_map_array();
        init_res_use_vector();
    }


    /**
     * 获得待迁移云盘原有的映射数组
     * @return 原位置映射数组
     */
    public int[] init_pre_map_array(){
        int[]  init_array = new int[diskList.size()];
        for(int i=0;i<init_array.length;i++){
            init_array[i] = warehouseList.indexOf(diskList.get(i).getHost());
        }
        return init_array;
    };

    public void init_res_use_vector(){
        this.warehouse_util_state =  new double[warehouseList.size()][5];
        this.disk_util_vector =  new double[diskList.size()][5];
        for(int i=0;i<warehouseList.size();i++){ //得到所有仓库的资源使用向量
            warehouse_util_state[i] = warehouseList.get(i).getWarehouse_Res_Vector(); //每一行表示一个仓库的资源利用率向量
        }
        for(int i=0;i<diskList.size();i++){
            disk_util_vector[i] = diskList.get(i).getAllDimUtilizationMean();
        }
    }
    /**
     * 获得存储集群目前的平均资源使用向量 <>Cap,read_IOPS,read_Bandwidth,Write_IOPS,Write_Bandwidth</>
     * 平均资源使用定义：前n个时间戳 仓库资源利用率的均值
     * 复杂度：O(n) n为仓库数量
     * @param hosts 当前系统中的所有仓库
     * @return 系统平均资源利用率
     */
    public double[] getBalanceStandard(List<Warehouse> hosts){
        double balance = 0;
        double[] balance_all_dim =  new double[5];
        double[][] warehouse_load = new double[hosts.size()][5];
        double[][] selected_wh_array = new double[1][hosts.size()];
        for(int i = 0; i< hosts.size();i++){
            selected_wh_array[0][i] = 1;
        }
        for(int i=0;i< hosts.size();i++){
            Warehouse wh = hosts.get(i);
            warehouse_load[i] = wh.getWarehouse_Res_Vector();
        }
        RealMatrix load_matrix = new Array2DRowRealMatrix(warehouse_load);
        RealMatrix selected_wh_matrix = new Array2DRowRealMatrix(selected_wh_array);
        RealMatrix system_av_load = selected_wh_matrix.multiply(load_matrix); //[]
        for(int i=0;i<5;i++){
            system_av_load.setEntry(0,i,system_av_load.getEntry(0,i)/hosts.size());
        }
        double [][] balance_standard = system_av_load.getData();
        return balance_standard[0];

    }

    /**
     * map_array: 对应云盘是否迁出的状态列表  0:迁出 1:保持当前位置
     * @return 迁出对应数量云盘后的适应度
     */
    public double balance_fitness_cal(double[] map_array,Warehouse wh) {
        double fit = 0;
        List<Vm> vms = wh.getVmList();
        vms.removeAll(wh.getVmsMigratingOut());
        vms.removeAll(wh.getVmsMigratingIn());
        double[][] vm_load_matrix = new double[vms.size()][5];
        double[][] vm_map_matrix = new double[1][map_array.length];
        vm_map_matrix[0] = map_array;
        for(int i=0;i<vms.size();i++) {
            CloudDisk cd = (CloudDisk) vms.get(i);
            double[] res_use_vector = cd.getAllDimUtilizationMean();
            vm_load_matrix[i] = res_use_vector;
        }
        RealMatrix load_matrix = new Array2DRowRealMatrix(vm_load_matrix);
        RealMatrix map_matrix = new Array2DRowRealMatrix(vm_map_matrix);
        RealMatrix warehouse_load = map_matrix.multiply(load_matrix); // warehouse_load 该仓库迁出后的仓库平均负载 1*5矩阵
        double[] system_res_util_vec = getBalanceStandard(this.warehouseList);
        for(int i=0;i<system_res_util_vec.length;i++){
            fit += Math.abs(system_res_util_vec[i] - warehouse_load.getEntry(0,i));
        }
        return  fit;
    }


    /**
     * map_array: 云盘对应装箱位置 map_array[i] = j 表示第i号云盘的装入位置为 j
     * 复杂度O(m)+O(n) = O(m)  主要受盘数影响
     * @return 重新布局后的系统均衡度
     */
    public double Cost_Effective_Fitness(int[] map_array) {
        double cost_fit = 0;
        double qos_fit = 0;
        double qos_constraint;

        List<CloudDisk> migration_list = this.diskList;
        List<Warehouse> warehouse_list = this.warehouseList;
        double[][] warehouse_util_vector = new double[warehouse_list.size()][5]; //所有仓库的状态
        for(int i=0;i<warehouse_util_vector.length;i++){
            warehouse_util_vector[i] = this.warehouse_util_state[i].clone();
        }
        for(int i=0;i<warehouse_list.size();i++){ //得到所有仓库的资源使用向量
            warehouse_util_vector[i] = warehouse_list.get(i).getWarehouse_Res_Vector(); //每一行表示一个仓库的资源利用率向量
        }
        for(int i=0;i<map_array.length;i++){ //遍历所有待迁移云盘 复杂度高的原因.....
            int dest_index= map_array[i];
            int pre_index = pre_map_array[i];
            if (pre_index != dest_index){
                cost_fit += migration_list.get(i).getCurrentAllocatedCap();
            }
//            double[] cd_res_use_vec = migration_list.get(i).getAllDimUtilizationMean(); // 该云盘的资源使用向量
            double[] cd_res_use_vec = disk_util_vector[i];
            for(int j=0;j<warehouse_util_vector[0].length;j++){ // 遍历所有维度
                warehouse_util_vector[pre_index][j] -= cd_res_use_vec[j]; //原仓库减去相关负载
                warehouse_util_vector[dest_index][j] += cd_res_use_vec[j]; //目的仓库增加相关负载

            }
        }
        double[] system_res_util_vec = new double[warehouse_util_vector[0].length]; // 系统平均资源利用率向量
        int warehouse_num = warehouse_util_vector.length;
        for(int i=0;i<warehouse_num;i++){
            system_res_util_vec[0] += warehouse_util_vector[i][0]/warehouse_num;
            system_res_util_vec[1] += warehouse_util_vector[i][1]/warehouse_num;
            system_res_util_vec[2] += warehouse_util_vector[i][2]/warehouse_num;
            system_res_util_vec[3] += warehouse_util_vector[i][3]/warehouse_num;
            system_res_util_vec[4] += warehouse_util_vector[i][4]/warehouse_num;
        }

        for(int i=0;i<warehouse_util_vector.length;i++){
            for(int j=0;j<system_res_util_vec.length;j++){
                qos_fit += Math.abs(system_res_util_vec[j] - warehouse_util_vector[i][j]); //整体不均衡度 fit越大表示适应度越差
            }
        }
        if (qos_fit > balance_qos_contraint * 2 *warehouse_num){ // 如果超过均衡度上线
            qos_fit = Double.MAX_VALUE;
        }else{
            qos_fit = 0;
        }
        return qos_fit + cost_fit ;
    }
    /**
     * 模拟从a仓库迁移到b仓库后的全局适应度情况
     * @param cd 迁移云盘
     * @param pre_index 原仓库
     * @param dest_index 目的仓库
     * @return 全局适应度
     */
    public double updataBalance(CloudDisk cd,int pre_index,int dest_index){
        double fit = 0;
        List<Warehouse> warehouse_list = this.warehouseList;
        double[][] warehouse_util_state = new double[warehouse_list.size()][5]; //所有仓库的状态
        for(int i=0;i<warehouse_list.size();i++){ //得到所有仓库的资源使用向量
            warehouse_util_state[i] = warehouse_list.get(i).getWarehouse_Res_Vector();
        }

        double[] cd_res_use_vec = cd.getAllDimUtilizationMean();
        for(int j=0;j<warehouse_util_state[j].length;j++){ // 遍历所有维度
            warehouse_util_state[pre_index][j] -= cd_res_use_vec[j]; //原仓库减去相关负载
            warehouse_util_state[dest_index][j] += cd_res_use_vec[j]; //目的仓库增加相关负载
        }
        double[] system_res_util_vec = getBalanceStandard(this.warehouseList);
        for(int i=0;i<warehouse_util_state.length;i++){
            for(int j=0;j<system_res_util_vec.length;j++){
                fit += Math.abs(system_res_util_vec[j] - warehouse_util_state[i][j]); //整体不均衡度 fit越大表示适应度越差
            }
        }
        return fit;
    }

    /**
     * 模拟退火启发式算法,通过disk_list待迁移云盘列表和warehouse_list可迁入云盘列表
     * 复杂度O(t*iter*m) ,复杂度与待迁移云盘数量、降温次数以及每个温度下的迭代次数成线性关系
     *
     * @return 最优的迁移map
     */
    public int[] minimizeCostBeyondConstraint(){
        Random random = new Random();
        int[] current_map = this.pre_map_array; // 记录当前探索的解
        double current_fitness = 0;  // 记录当前探索解的适应度
        int[] best_map = this.pre_map_array; // 记录探索过程中的最优解
        double best_fitness = Cost_Effective_Fitness(current_map); // 记录探索过程中的最优适应度
        double ex_fitness = best_fitness; //记录上一代的探索解
        int iteration = 0; //每次降温的迭代次数
        double t = init_temp; // 初始温度 100度
        double annel_time = 0; // 降温次数
        while (annel_time <T_desc) { // 小于降温的次数 不断循环降温
            iteration = 0; // 迭代代数
            while(iteration < iteration_limit) { //每次降温迭代相应次数
                int[] random_map = randomMap(); // 探索解的方向，目前为随机探索
                current_fitness = Cost_Effective_Fitness(random_map); // 计算 当前探索解的适应度
//                current_fitness = random.nextFloat();
                if(current_fitness<best_fitness){ // 如果小于当前最佳适应度 ，代表当前解更好。
                    best_fitness = current_fitness;
                    current_map = random_map;
                    best_map = current_map;
                }
                double delt=current_fitness - ex_fitness; // 当前解和上次解的适应度差值
                if(delt>0 && Math.exp(delt/t)>random.nextFloat()){ // 差值大于0，代表适应度有所下降，以一定概率接受新解
                    ex_fitness = current_fitness;
                    current_map = random_map;
                }
                iteration+=1; // 代数加1
            }
            t = a*t; // 降温
            annel_time +=1 ; // 退火次数加1
        }
        return current_map;
    }

    /**
     * 随机探索新的迁移映射，目前的探索为全随机
     * @return 随机的迁移映射解
     */
    public int[] randomMap(){
        int[] migration_map = new int[this.diskList.size()];
        int warehouse_size = this.warehouseList.size();
        Random random = new Random();
        for(int i=0;i<migration_map.length;i++){
            int dest_index = random.nextInt(warehouse_size);
            migration_map[i] = dest_index;
        }
        return  migration_map;
    }
    public List<Vm> SelectMigrationDisk(Warehouse wh) {
        List<Vm> vms = wh.getVmList();
        List<Vm> selected_vms = new ArrayList<Vm>();
        int[] map_array = new int[vms.size()];
        return selected_vms;
    }
}
