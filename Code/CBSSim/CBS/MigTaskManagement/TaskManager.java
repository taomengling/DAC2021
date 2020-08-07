package org.cloudbus.cloudsim.examples.CBS.MigTaskManagement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TaskManager {
    public int migration_bw_limit;//迁移带宽上限，表示迁移任务的可用总带宽 单位MB
    public Management_Strategy strategy; //迁移任务管理策略:1.立即执行 2、固定时间执行 3、动态自适应执行
    public List<Map<String,Object>> task_list = new LinkedList<Map<String, Object>>();
    public TaskManager(int Migration_Bw,Management_Strategy strategy){
        setMigration_bw_limit(Migration_Bw);
        setStrategy(strategy);
    }
    public int getMigration_bw_limit() {
        return migration_bw_limit;
    }

    public void setMigration_bw_limit(int migration_bw_limit) {
        this.migration_bw_limit = migration_bw_limit;
    }

    public Management_Strategy getStrategy() {
        return strategy;
    }

    public void setStrategy(Management_Strategy strategy) {
        this.strategy = strategy;
    }
}
