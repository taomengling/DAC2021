package org.cloudbus.cloudsim.examples.CBS.HeuristicAlg;

import org.cloudbus.cloudsim.entity.CloudDisk;
import org.cloudbus.cloudsim.entity.Warehouse;
import org.cloudbus.cloudsim.examples.CBS.HeuristicAlg.GA.Chromosome;
import org.cloudbus.cloudsim.examples.CBS.HeuristicAlg.GA.GeneticAlgorithm;

import java.util.ArrayList;
import java.util.List;

/**
 * 多目标 遗传算法
 */
public class Modified_GA extends GeneticAlgorithm {
    double balance_qos_contraint = 0.01;
    List<CloudDisk> diskList = new ArrayList<>();
    List<Warehouse> warehouseList = new ArrayList<>();
    int[] pre_map_array;
    double[][] warehouse_util_state ;
    double[][] disk_util_vector;

    public Modified_GA(List<CloudDisk> diskList,List<Warehouse> warehouseList) {
        super(diskList.size(),warehouseList.size());
        this.diskList = diskList;
        this.warehouseList = warehouseList;
        this.pre_map_array = init_pre_map_array();
        init_res_use_vector();


    }
    public int[] init_pre_map_array(){
        int[]  init_array = new int[diskList.size()];
        for(int i=0;i<init_array.length;i++){
            init_array[i] = warehouseList.indexOf(diskList.get(i).getHost());
        }
        return init_array;
    }
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

    @Override
    public double calculateY(int[] map_array){
        if(map_array == null){
            return Double.MAX_VALUE;
        }
        double cost_fit = 0;
        double qos_fit = 0;
        double qos_constraint;

        List<CloudDisk> migration_list = this.diskList;
        List<Warehouse> warehouse_list = this.warehouseList;
        double[][] warehouse_util_vector = new double[warehouseList.size()][5];
        for(int i=0;i<warehouse_util_vector.length;i++){
            warehouse_util_vector[i] = this.warehouse_util_state[i].clone();
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
                if(Math.abs(system_res_util_vec[j] - warehouse_util_vector[i][j])>balance_qos_contraint && getFitTarget().equals("cost")){ // 如果超过均衡度上限进行惩罚
                    cost_fit = 1000000000;
                    break;
                }
                qos_fit += Math.abs(system_res_util_vec[j] - warehouse_util_vector[i][j]); //整体不均衡度 fit越大表示适应度越差
            }
        }
        if (qos_fit > balance_qos_contraint * 2 * warehouse_num) { // 如果超过均衡度上限进行惩罚
            cost_fit = 1000000000;
        }
        if (getFitTarget().equals("qos")){
            return 1/qos_fit;
        }else if(getFitTarget().equals("cost")){
            return 1/cost_fit;
        }
        return 0;

    }

    public int[] changeX(Chromosome chromosome){
        return chromosome.getGene();
    }

}
