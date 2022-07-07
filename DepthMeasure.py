
import pandas as pd
from scipy import signal

def get_high_point(ecg_1, in_idx):   # 查找极大点下标   # in_idx实际是下标
    while ecg_1[in_idx-1] - ecg_1[in_idx] > 0:
        in_idx -= 1
    return in_idx-1
def get_low_point(ecg_1, in_idx):   # 查找极小点下标   # in_idx实际是下标
    while ecg_1[in_idx-1] - ecg_1[in_idx] < 0:
        in_idx -= 1
    return in_idx-1
def get_two_extreme_index(ecg_1, plumb_show_idx):   # 返回极大点和极小点下标
    trend = ecg_1[plumb_show_idx-1] - ecg_1[plumb_show_idx]    # 上升为-, 下降为+    # plumb_show_idx实际是下标
    now_idx = plumb_show_idx
    if trend > 0:
        high_idx = get_high_point(ecg_1, now_idx)
        before_idx = high_idx-1
        low_idx = get_low_point(ecg_1, before_idx)
    else:
        low_idx = get_low_point(ecg_1, now_idx)
        high_idx = get_high_point(ecg_1, low_idx-1)
    return high_idx, low_idx   # 返回也是下标值
def get_middle_times(idx_list, ecg_1, plumb_index, middle_angle):
    in_idex = plumb_index
    middle_list=[]  # 保存过中点的下标
    gap=[]  # 过中点之间的间隔  (idx间隔)
    while in_idex > 0:   # 计算过中点下标
        if (ecg_1[in_idex]-middle_angle) * (ecg_1[in_idex-1]-middle_angle) < 0 :  #相乘小于0
            middle_list.append(in_idex)
        in_idex -= 1
    gap.append(idx_list[middle_list[0]]-idx_list[middle_list[1]])  # 填充初始间隔值(idx间隔)
#     middle_times = 0   # 次数
    for i in range(len(middle_list)):  # 遍历中点下标
        if i <= 1:  # 过滤前两次
            continue
        average_gap = sum(gap)/len(gap)  # 平均idx间隔
        now_gap = idx_list[middle_list[i-1]] - idx_list[middle_list[i]]
        if (abs(now_gap - average_gap))<(0.25 * average_gap):   # idx间隔变化不大
            gap.append(now_gap)
        else:    # 间隔变化较大时, 说明该位置不在量井动作中
            return i+1
    return len(middle_list)

def get_depth1(plumb_index, angle_list,idx_list):
    b, a = signal.butter(8, 0.2, 'lowpass')  # 配置滤波器 8 表示滤波器的阶数
    # b, a = signal.butter(8, [0.01,0.4], 'bandpass')
    ecg_1 = signal.filtfilt(b, a, angle_list)  # data为要过滤的信号
    plumb_index = idx_list.index(plumb_index )      # plumb_index实际是下标
    extreme_point1, extreme_point2 = get_two_extreme_index(ecg_1, plumb_index)  # 两个极值点的下标
    middle_angle = (ecg_1[extreme_point1] + ecg_1[extreme_point2]) / 2  # 中间值
    # 得到次数
    in_times = get_middle_times(idx_list, ecg_1, plumb_index, middle_angle)
    return in_times




#
# if __name__ == '__main__':
#     csvpath = r'G:\deep learning\zhizheng_porject_data\tese-pose-angle.csv'
#     data = pd.read_csv(csvpath)
#     abplumb_show_idx = 1437
#     b, a = signal.butter(8, 0.03, 'lowpass')  # 配置滤波器 8 表示滤波器的阶数
#     # b, a = signal.butter(8, [0.01,0.4], 'bandpass')
#     ecg_1 = signal.filtfilt(b, a, data.angle[:])  # data为要过滤的信号
#     plt.figure(figsize=(20, 8))
#     # plt.plot(rdata[:,1])
#     plt.subplot(211)
#     plt.plot(data.idx[:], data.angle[:])
#     plt.subplot(212)
#     plt.plot(data.idx[:], ecg_1)
#     # area1 = fig.add_subplot(2,1,1)
#     # plt.plot(data.idx[300:],ecg_1+130,data.idx[300:],data.angle[300:])
#     plt.show()
#     print(get_depth(abplumb_show_idx, ecg_1, data))
