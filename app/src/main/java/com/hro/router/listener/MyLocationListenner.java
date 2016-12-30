package com.hro.router.listener;

/**
 * Created by Mr Mo on 2016/11/26.
 */

import android.util.Log;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.hro.router.data.DataManager;
import com.hro.router.service.MyLocationService;
import com.hro.router.util.Constant;
import com.hro.router.util.LocationDistanceUtil;
import com.hro.router.util.StringUtil;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 定位SDK监听函数
 */
public class MyLocationListenner implements BDLocationListener {

    private DateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private MyLocationService myLocalService = null;
    private int smsSendCnt = 1;

    @Override
    public void onReceiveLocation(BDLocation location) {
        //销毁后不在处理新接收的位置
        if (location == null) {
            return;
        }
        boolean isMoving = true;
        double dist = 0;
        myLocalService = DataManager.getInstance().getMyLocalService();
        if( DataManager.getInstance().getLastLocation() != null ) {
            dist = LocationDistanceUtil.getShortDistance(DataManager.getInstance().getLastLocation().getLongitude(), DataManager.getInstance().getLastLocation().getLatitude(),
                    location.getLongitude(), location.getLatitude());
            if (dist > Constant.MAX_POINT_MOVING_DIST) {
                isMoving = true;

                if(myLocalService != null) {
                    StringBuffer strBf = new StringBuffer("");
                    strBf.append("经纬度：").append(location.getLongitude()+","+ location.getLatitude());
                    strBf.append(" 地址：").append(location.getAddrStr());
                    strBf.append(" 监控时间：").append(dFormat.format(new Date()));
                    String smsMsg = strBf.toString();

                    if(!myLocalService.sendTimMessage(smsMsg)) {
//                        myLocalService.sendSmsMessage(smsMsg);
                    }
                }

            } else {
                StringBuffer strBf = new StringBuffer("");
                strBf.append("经纬度：").append(location.getLongitude()+","+ location.getLatitude());
                strBf.append(" 地址：").append(location.getAddrStr());
                strBf.append(" 监控时间：").append(dFormat.format(new Date()));
                String smsMsg = strBf.toString();

                myLocalService.sendTimMessage(smsMsg);

                isMoving = false;
            }
        }

//      Log.i(this.getClass().getName(), " Lng->"+location.getLongitude()+" Lat->"+ location.getLatitude() );

        if(DataManager.getInstance().getBMap() != null ){
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            DataManager.getInstance().getBMap().setMyLocationData(locData);
        }

        if ( Constant.isMyLocalTrace && isMoving ) {
            LatLng ll = new LatLng(location.getLatitude(),
                    location.getLongitude());
//            city = location.getCity();
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(ll).zoom(Constant.MAP_DEFAULT_ZOOM);
            if(DataManager.getInstance().getBMap() != null ) {
                DataManager.getInstance().getBMap().animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
            DataManager.getInstance().setLastLocation(location);
        }

    }

    public void onReceivePoi(BDLocation poiLocation) {

        //Receive Location
        //经纬度
        double lati = poiLocation.getLatitude();
        double longa = poiLocation.getLongitude();
        //打印出当前位置
        Log.i("TAG", "location.getAddrStr()=" + poiLocation.getAddrStr());
        //打印出当前城市
        Log.i("TAG", "location.getCity()=" + poiLocation.getCity());
        //返回码
        int i = poiLocation.getLocType();
    }
}