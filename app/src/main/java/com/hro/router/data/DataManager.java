package com.hro.router.data;

import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.mapapi.map.BaiduMap;
import com.hro.router.service.MyLocationService;
import com.hro.router.service.WatchDogService;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Mr Mo on 2016/11/13.
 */

public class DataManager {

    private static DataManager instance = null;

    private BaiduMap mBaiduMap = null;
    private LocationClient mLocClient = null;      //位置服务

    private MyLocationService myLocalService = null;
    private WatchDogService watchDogService = null;
    private BDLocation lastLocation = null; //记录最后一次位置

    private DataManager(){}

    private static class HolderClass{
        private static DataManager manager = new DataManager();
    }

    public static DataManager getInstance(){
        if(instance == null){
            instance = HolderClass.manager;
        }
        return instance;
    }

    public void setBMap(BaiduMap mBaiduMap){
        this.mBaiduMap = mBaiduMap;
    }

    public BaiduMap getBMap(){
        return this.mBaiduMap;
    }

    public void setMyLocalService(MyLocationService myLocalService){
        this.myLocalService = myLocalService;
    }

    public MyLocationService getMyLocalService(){
        return this.myLocalService;
    }

    public void setWatchDogService(WatchDogService watchDogService){
        this.watchDogService = watchDogService;
    }

    public WatchDogService getWatchDogService(){
        return this.watchDogService;
    }

    public void setmLocClient(LocationClient mLocClient){
        this.mLocClient = mLocClient;
    }

    public LocationClient getmLocClient(){
        return mLocClient;
    }

    public BDLocation getLastLocation() {
        return lastLocation;
    }

    public void setLastLocation(BDLocation lastLocation) {
        this.lastLocation = lastLocation;
    }

}
