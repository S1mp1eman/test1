#!/usr/bin/env python
# -*-coding: utf-8 -*-

import csv
import math
import time

import cv2
import numpy as np
import torch

import DepthMeasure
import posenet
from yolov3.darknet import darknet


# darknet网络模型
class dark_model():
    # 初始化,传入configPath   weightPath   metaPath
    def __init__(self, configPath, weightPath, metaPath):
        # self.net = darknet.load_net(configPath.encode('utf-8'), weightPath.encode('utf-8'), 0)
        self.network, self.class_names, self.class_colors = darknet.load_network(
            configPath,
            metaPath,
            weightPath,
            batch_size=1
        )

    # 推理图片, 返回检测到的目标
    def predict(self, image):
        # 图片处理
        width = darknet.network_width(self.network)
        height = darknet.network_height(self.network)
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)  # 转rgb
        image_resized = cv2.resize(image, (width, height), interpolation=cv2.INTER_LINEAR)
            # darknet.letterbox_image(image, (width, height))
        # 计算缩放比例
        scale_h = image.shape[0] / image_resized.shape[0]
        scale_w = image.shape[1] / image_resized.shape[1]
        # yolov4 darknet 处理图片
        darknet_image = darknet.make_image(width, height, 3)
        darknet.copy_image_from_bytes(darknet_image, image_resized.tobytes())
        # 推理
        detections = darknet.detect_image(self.network, self.class_names, darknet_image, thresh=0.4)
        darknet.free_image(darknet_image)

        # detections处理
        # detections = np.array(detections)


        # detections 还原到原图像size
        rep = {'box': [], 'score': [], 'clas': []}
        for detection in detections:
            # 还原到原图像size
            rep["box"].extend([detection[2][0] * scale_w, detection[2][1] * scale_h,
                               detection[2][2] * scale_w, detection[2][3] * scale_h])
            rep["score"].extend([detection[1]])
            rep["clas"].extend([str(detection[0])])
        return rep

class posenet_model():
    def __init__(self):     # 初始化
        self.model = posenet.load_model(101)
        # self.output_stride = self.model.output_stride = posenet.load_model(args.model)
        self.model = self.model.cuda()
        self.output_stride = self.model.output_stride

    def predict(self, image):       # 推理图片, 返回检测到的目标
        input_image, draw_image, output_scale = posenet.read_imgfile(image, scale_factor=0.7125)
        with torch.no_grad():
            input_image = torch.Tensor(input_image).cuda()
            heatmaps_result, offsets_result, displacement_fwd_result, displacement_bwd_result = self.model(input_image)
            pose_scores, keypoint_scores, keypoint_coords = posenet.decode_multiple_poses(
                heatmaps_result.squeeze(0),
                offsets_result.squeeze(0),
                displacement_fwd_result.squeeze(0),
                displacement_bwd_result.squeeze(0),
                output_stride=self.output_stride,
                max_pose_detections=10,
                min_pose_score=0.25)
        keypoint_coords *= output_scale
        draw_image = posenet.draw_skel_and_kp(
            draw_image, pose_scores, keypoint_scores, keypoint_coords,
            min_pose_score=0.01, min_part_score=0.01)
        return pose_scores, keypoint_scores, keypoint_coords, draw_image

def convertBack(x, y, w, h):  # 中心点-> 矩形框
    xmin = int(round(x - (w / 2)))
    xmax = int(round(x + (w / 2)))
    ymin = int(round(y - (h / 2)))
    ymax = int(round(y + (h / 2)))
    return [abs(xmin), abs(ymin), abs(xmax), abs(ymax)]


''' ##################################### 钻井方法 ##################################### '''
def record_movemnet(x1, y1, x2, y2, clas, Coordinate_hook, Coordinate_drill):  # 钻头,叉子作为主要量井判断, 记录他们的中心点位置
    if clas == 'drill':  # 钻头的中心点位置
        Coordinate_drill.append([(x1 + x2) / 2, (y1 + y2) / 2])
    elif clas == 'hook':  # 叉子的中心点位置
        Coordinate_hook.append([(x1 + x2) / 2, (y1 + y2) / 2])
    return Coordinate_hook, Coordinate_drill

def detect_movement(clas, coordinate, Movement_bin_hook_V):  # coordinate为中心点位置
    for i in range(len(coordinate)):
        diff_x = coordinate[i][0] - coordinate[i - 1][0]
        diff_y = coordinate[i][1] - coordinate[i - 1][1]  # 判断钩子中中心点上下位移数据
        if diff_y < -2:
            Movement_bin_hook_V.append(- diff_y)
        elif diff_y > 2:
            Movement_bin_hook_V.append(- diff_y)
        else:
            Movement_bin_hook_V.append(0)
    print("Movement_bin_hook_V")
    print(Movement_bin_hook_V)
    binsize = 3
    movemnet_tmp = []
    for i in range(len(Movement_bin_hook_V)):
        if i % binsize == 0:
            dir = 0
            for j in range(binsize):
                dir += Movement_bin_hook_V[i - j]
            if dir >= 5:
                movemnet_tmp.append(+1)
            # elif dir <= -1:
            #     movemnet_tmp.append(-1)
            else:
                movemnet_tmp.append(0)
    print(len(movemnet_tmp))
    print(movemnet_tmp)
    return movemnet_tmp

def count_up_times(movemnet):
    times = []
    confidence = 0
    for i in range(len(movemnet)):
        if movemnet[i] == 1:
            confidence += 1
        else:
            if (confidence >= 3):
                times.append('Up')
            # if (confidence >= 5 ):
            #     #for i in range (int(confidence / 5)):
            #         times.append('Up')
            confidence = 0
    print(times)
    return times
''' ##################################### End 钻井方法 ##################################### '''

''' ##################################### 量井方法 ##################################### '''
def keyPoint_to_angle(idx, pose_scores:np.ndarray, lastwise_coordinate, keypoint_coords:list, angle_buffer):
    manID = np.where(pose_scores == max(pose_scores))     # 获得score分数组大的人
    leftwise = np.array(keypoint_coords[manID, 9, : ])[0][0]     # 获得两个手腕的坐标
    rigthwise = np.array(keypoint_coords[manID, 10, : ])[0][0]
    print("左手腕:{}; 右手腕{}".format(leftwise, rigthwise))
    Xzhou = [0, 1]  # X轴
    if leftwise.all() == 0. and rigthwise.all() == 0.:
        letftoright = lastwise_coordinate[1] - lastwise_coordinate[0]  # 向量
        wise_coordinate = lastwise_coordinate   # 漏检时保存之前的坐标
    else:
        letftoright = rigthwise - leftwise      # 向量
        wise_coordinate = np.array([leftwise, rigthwise])  # 不漏检,更新上一帧坐标
    angle_wise = arcsin_and_arccos(letftoright, Xzhou)      # 计算角度
    if idx <= 3:
        angle_buffer[idx-1] = angle_wise        # 前3帧填充
    else:
        if abs(sum(angle_buffer)/3 - angle_wise) > 200:
            if angle_wise > 180:
                angle_wise -= 360
            else:
                angle_wise += 360
        angle_buffer[(idx-1)%3] = angle_wise
    return angle_wise,angle_buffer,wise_coordinate      # 返回角度,角度缓存,坐标

# 计算两个向量角度 输出0-360度
def arcsin_and_arccos(pt1, pt2):
    delta_x = pt2[0] - pt1[0]
    delta_y = pt2[1] - pt1[1]
    sin = delta_y/math.sqrt(delta_x**2 + delta_y**2)
    cos = delta_x/math.sqrt(delta_x**2 + delta_y**2)
    if sin >= 0 and cos >= 0:
        return math.asin(sin) / math.pi * 180
    elif sin >= 0 and cos < 0:
        return math.acos(cos) / math.pi * 180
    elif sin < 0 and cos < 0:
        return (2 * math.pi - math.acos(cos)) / math.pi * 180
    elif sin < 0 and cos >= 0:
        return (2 * math.pi - math.acos(cos)) / math.pi * 180
''' ##################################### End 量井方法 ##################################### '''


if __name__ == '__main__':
    md_flag = True      #铅锤出现后为false
    is_dection = False      # hook出现后经过一段时间为true
    idx = 0  # 帧数
    fork_sign = 0   # fork标志
    fork_time = time.time()  # fork出现时间
    plumbidx = 0
    plumb_count = 3     # 铅锤出现2次,后说明铅锤出现(过滤误检)
    Coordinate_hook, Coordinate_drill, Movement_bin_hook_V = [], [], []  # 记录hook和dirll和的坐标

    safe_sign = [0,0,0,0]       # 记录 worker,hat, medikit, extinguisher的次数
    idx_list = []
    angle_list = []
    lastwise_coordinate = np.array([[0,0],[0,0]])
    angle_buffer = [0,0,0]
    skip = 8    # 跳帧

    video_path = r"G:\标准钻井视频\现场拍摄\3.mp4"
    cap = cv2.VideoCapture(video_path)
    fps = int(cap.get(cv2.CAP_PROP_FPS))
    frame_count = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    frame_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
    frame_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))

    configPath = "./liangjing/clean-tiny-3l.cfg"
    weightPath = "./liangjing/clean-tiny-3l_best.weights"
    metaPath = "./liangjing/clean-liangjing.data"
    darknet_model = dark_model(configPath, weightPath, metaPath)
    posenet_model = posenet_model()

    # f = open("hook_drill_list.csv", 'w', encoding='utf-8', newline="")  # csv保存文件
    # CSVfile = csv.writer(f)  # csv文件
    # CSVfile.writerow(['idx', 'hooky', 'drilly',"angle"])  # 写入表头


    while cap.isOpened():
        ret, frame = cap.read()
        if not ret:
            print("no frame")
            break
        idx = idx + 1
        if md_flag and idx % skip==0:
            # 图片处理
            rep = darknet_model.predict(frame)
            classes = rep["clas"]  # 类别
            scores = rep["score"]  # 置信度
            boxes = np.array(rep["box"])  # 预测框位置
            boxes = boxes.reshape((-1, 4))

            drilly, hooky = 0, 0

            for clas, score, box in zip(classes, scores, boxes):
                score = float(score)
                if float(score) >= 50.00:
                    # TODO
                    x1, y1, x2, y2 = convertBack(box[0], box[1], box[2], box[3])
                    x1, y1, x2, y2 = int(x1), int(y1), int(x2), int(y2)
                    # 画框
                    cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
                    cv2.putText(frame, clas, (x1, y1), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
                    if clas == 'fork':
                        if fork_sign ==0:
                            fork_sign = 1
                            fork_time = time.time()

                    elif clas == 'plumb':
                        if is_dection:
                            plumbidx = int(idx/skip)
                            # plumbbox = box
                            # if plumb_count>0:
                            #     plumb_count = plumb_count - 1
                            # else:
                            #     cv2.destroyAllWindows()
                            #     cv2.namedWindow("plumb", 0);
                            #     cv2.resizeWindow("plumb", 450, 800);
                            #     cv2.imshow('plumb', frame)
                            #     plumbidx = int(idx/skip)
                            #     md_flag = False
                    elif clas == 'drill' or 'hook':
                        # TODO
                        if clas == 'drill':
                            drilly = box[1]
                        elif clas == 'hook':
                            hooky = box[1]
                        Coordinate_hook, Coordinate_drill = record_movemnet(x1, y1, x2, y2, clas, Coordinate_hook, Coordinate_drill)

                    elif clas == 'worker':
                        safe_sign[0] = safe_sign[0] + 1
                    elif clas == 'hat':
                        safe_sign[1] = safe_sign[1] + 1
                    elif clas == 'medikit':
                        safe_sign[2] = safe_sign[2] + 1
                    elif clas == 'extinguisher':
                        safe_sign[3] = safe_sign[3] + 1
            # mytime = time.time()
            if fork_sign == 1 and time.time()-fork_time > 30:
                is_dection = True
                pose_scores, keypoint_scores, keypoint_coords, frame  = posenet_model.predict(frame)
                angle, angle_buffer,wise_coordinate = keyPoint_to_angle(idx, pose_scores, lastwise_coordinate,
                                                                          keypoint_coords, angle_buffer)
                angle_list.append(angle)
                idx_list.append(int(idx/skip))
                # CSVfile.writerow([int(idx/skip), 0, 0, angle])
                # CSVfile.writerow([int(idx/skip), drilly, hooky, angle])

        if idx% skip == 0 and ret:
            cv2.namedWindow("posenet", 0);
            cv2.resizeWindow("posenet", 450, 800);
            cv2.imshow('posenet', frame)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

    # f.close()
    cap.release()
    '''计算部分'''
    # 计算安全标志, 判断safe_sign是否都大于100
    if safe_sign[0] > 100 and safe_sign[1] > 100 and safe_sign[2] > 100 and safe_sign[3] > 100:
        print('-------------------safe is true---------------')
    # 计算钻井深度
    Movement_bin_hook_V = detect_movement('hook', Coordinate_hook, Movement_bin_hook_V)
    times = count_up_times(Movement_bin_hook_V)
    print("钻井深度为:", times)
    # 计算量井深度
    if plumbidx==0:
        plumbidx = int(idx/skip)
    fork_depth = DepthMeasure.get_depth1(plumbidx, angle_list,idx_list)
    print("量井深度为:", fork_depth)
    pass





