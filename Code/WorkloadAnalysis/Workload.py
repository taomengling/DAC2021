import numpy as np
import pandas as pd


class Workload:
    id = ""
    disk_info = []
    capacity_curve = []
    readIOPSCurve = []
    writeIOPSCurve = []
    readBwCurve = []
    writeBwCurve = []

    def __init__(self, workload_id, disk_info , resource_use_trace):
        self.id = workload_id
        self.disk_info = disk_info
        self.capacity_curve = resource_use_trace[0]
        self.readIOPSCurve = resource_use_trace[1]
        self.writeIOPSCurve = resource_use_trace[2]
        self.readBwCurve = resource_use_trace[3]
        self.writeBwCurve = resource_use_trace[4]



    def getAverageReadIOPS(self):
        return np.mean(self.readIOPSCurve)

    def getAverageWriteIOPS(self):
        return np.mean(self.writeIOPSCurve)

    def getAverageReadBw(self):
        return np.mean(self.readBwCurve)

    def getAverageWriteBw(self):
        return np.mean(self.writeBwCurve)

    def getUsageCapacity(self):
        return self.capacity_curve[-1]
