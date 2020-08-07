import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from Workload import Workload
import os
import time

dataSetDir = "F://DAC2021/DataSet/"
start_time_stamp = 1591588800

def time_str2stamp(time_str):   # 字符时间转时间戳
    timeArray = time.strptime(time_str, "%Y-%m-%d %H:%M:%S")
    timeStamp = int(time.mktime(timeArray))
    return timeStamp


def getWorkloadListFromDataSet():
    """
    获得所有云盘信息
    """
    workload_list = []
    warehouse_name_list = []
    for i in os.listdir(dataSetDir):
        if os.path.isdir(os.path.join(dataSetDir, i)):
            warehouse_name_list.append(i)
    for wh_name in warehouse_name_list:
        start_time = time.perf_counter_ns()
        warehouse_dir = os.path.join(dataSetDir, wh_name)
        cloud_disk_name_list = os.listdir(warehouse_dir)
        cloud_disk_name_list = [i for i in cloud_disk_name_list if len(i) == 36]  # 获取所有有效的云盘名
        for cd_name in cloud_disk_name_list:
            cloud_disk_dir = os.path.join(warehouse_dir, cd_name)
            f = open(cloud_disk_dir)
            lines = f.readlines()
            cap = []
            riops = []
            wiops = []
            rbw = []
            wbw = []
            for line in lines:
                cap.append(line[-1])
                riops.append(line[1])
                wiops.append(line[2])
                rbw.append(line[3])
                wbw.append(line[4])
            workload_list.append(Workload(cd_name, [cap, riops, wiops, rbw, wbw]))
        end_time = time.perf_counter_ns()
        print("读取仓库" + wh_name + "花费时间:" + str(end_time - start_time))
        print("读取云盘数量:" + str(len(cloud_disk_name_list)))
    return workload_list


def getNewCreateWorkloadListFromDataSet():
    workload_list = []
    warehouse_name_list = []
    for i in os.listdir(dataSetDir):
        if os.path.isdir(os.path.join(dataSetDir, i)):
            warehouse_name_list.append(i)
    for wh_name in warehouse_name_list:
        start_time = time.perf_counter()
        warehouse_dir = os.path.join(dataSetDir, wh_name)
        try:
            cloud_disk_info = pd.read_csv((str(warehouse_dir) + '/' + wh_name.split("_")[1] + "_diskinfo")).values
        except FileNotFoundError:
            print("disk info file missing!")
            continue
        new_cloud_disk_info_list = [i for i in cloud_disk_info if
                                    time_str2stamp(i[4]) > time_str2stamp("2020-06-09 00:00:00")]  # 选择所有最近创建的云盘
        new_cloud_disk_info_list = np.array(new_cloud_disk_info_list)
        count = 0
        for cd_name in new_cloud_disk_info_list[:, 1]:
            cloud_disk_dir = os.path.join(warehouse_dir, cd_name)
            disk_info = new_cloud_disk_info_list[count, :]
            try:
                f = open(cloud_disk_dir)
            except FileNotFoundError:
                print("disk trace file missing!")
                continue

            cap = []
            riops = []
            wiops = []
            rbw = []
            wbw = []
            for line in f:
                # print(line)
                line = line[:-1]
                data = line.split(',')
                cap.append(data[-1])
                riops.append(data[1])
                wiops.append(data[2])
                rbw.append(data[3])
                wbw.append(data[4])

            workload_list.append(Workload(cd_name, disk_info, [cap, riops, wiops, rbw, wbw]))
        end_time = time.perf_counter()
        print("读取仓库" + wh_name + "花费时间:" + str(end_time - start_time))
        print("读取云盘数量:" + str(len(new_cloud_disk_info_list)))
        break
    return workload_list


def AnalysisWorkloadCapacityFeature(workloads):
    for workload in workloads:
        print(workload.disk_info)
        all_dim = [workload.capacity_curve,
                   workload.readIOPSCurve,
                   workload.writeIOPSCurve,
                   workload.readBwCurve,
                   workload.writeBwCurve]
        create_time_stamp = time_str2stamp(workload.disk_info[4])
        for i in range(5):
            res = [int(i) for i in all_dim[i]]
            plt.subplot(5, 1, i+1)
            plt.vlines((create_time_stamp - start_time_stamp) / 300, 0, np.max(res))
            plt.plot(res)
            plt.vlines((create_time_stamp - start_time_stamp)/300, 0, np.mean(res))
        plt.show()

if __name__ == "__main__":
    workload_list = getNewCreateWorkloadListFromDataSet()
    AnalysisWorkloadCapacityFeature(workload_list)