package com.hro.router.util;

/**
 * Created by Mr Mo on 2016/11/2.
 */

public class Constant {

    public static boolean isMapTouch = false;   //是否触控地图
    public static boolean isMyLocalTrace = true;  //是否定位我的位置
    public static boolean isMovingTrace = false; //是否监控我的移动(后续用于车辆监控)
    public static float MAX_POINT_MOVING_DIST = 5;  //侦测"点"的最大移动距离为20米
    public static float MAP_DRAG_DIST = 500; //用于观察是否拖动了百度地图图层50米
    public static int scanTime = 2000; //位置扫描时间间隔
    public static float MAP_DEFAULT_ZOOM = 18.0f; //地图默认级别
    public static String ALARM_PHONE = "";  //联动手机
    public static String TIM_ACCOUNT = "";  //云信帐号

    public static final int NOTIFY_MAPLOCATE_ID = 1001;
}
