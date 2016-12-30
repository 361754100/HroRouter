package com.hro.router.service;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.hro.router.MapMainActivity;
import com.hro.router.R;
import com.hro.router.data.DataManager;
import com.hro.router.event.MessageEvent;
import com.hro.router.listener.MyLocationListenner;
import com.hro.router.util.Constant;
import com.hro.router.util.StringUtil;
import com.hro.router.util.Utils;
import com.tencent.TIMCallBack;
import com.tencent.TIMConnListener;
import com.tencent.TIMConversation;
import com.tencent.TIMConversationType;
import com.tencent.TIMElem;
import com.tencent.TIMElemType;
import com.tencent.TIMLogListener;
import com.tencent.TIMManager;
import com.tencent.TIMMessage;
import com.tencent.TIMMessageListener;
import com.tencent.TIMTextElem;
import com.tencent.TIMUser;
import com.tencent.TIMUserStatusListener;
import com.tencent.TIMValueCallBack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Mr Mo on 2016/10/31.
 */

public class MyLocationService extends Service {

    private final IBinder mBinder = new LocalBinder();
    private ActivityManager activityManager=null;
    private PackageManager packageManager=null;
    private PackageInfo pi = null;
    private static final String ProcessName = "com.hro.router";
    private TIMConversation conversation = null;

    // 定位相关
    private LocationClient mLocClient = null;      //位置服务
    private String city	="";                //当前城市

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    // 与界面交互的类，由于service跟界面总是运行在同一程序里，所以不用处理IPC
    public class LocalBinder extends Binder {
        MyLocationService getService() {
            return MyLocationService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.setForeground("HroRouter", "程序正在运行");
        this.initLocateClient();
        DataManager.getInstance().setMyLocalService(this);
//        this.initBaiduPush();

        this.initTimListener();
        this.timLogin();
        this.initConversation();

        return START_STICKY;
    }

    private void setForeground(String title, String msgTxt){
        //步骤1：和上一笔记一样，通过Notification.Builder( )来创建通知
        //FakePlayer就是两个大button的activity，也即服务的界面，见最左图
        Intent i = new Intent(this, MapMainActivity.class);
        //注意Intent的flag设置：FLAG_ACTIVITY_CLEAR_TOP: 如果activity已在当前任务中运行，在它前端的activity都会被关闭，它就成了最前端的activity。FLAG_ACTIVITY_SINGLE_TOP: 如果activity已经在最前端运行，则不需要再加载。设置这两个flag，就是让一个且唯一的一个activity（服务界面）运行在最前端。
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);
        Notification myNotify = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
//                .setTicker("HroRouter" )
                .setContentTitle(title)
                .setContentText(msgTxt)
                .setContentIntent(pi)
                .build();
        //设置notification的flag，表明在点击通知后，通知并不会消失，也在最右图上仍在通知栏显示图标。这是确保在activity中退出后，状态栏仍有图标可提下拉、点击，再次进入activity。
        myNotify.flags |= Notification.FLAG_NO_CLEAR;

        // 步骤 2：startForeground( int, Notification)将服务设置为foreground状态，使系统知道该服务是用户关注，低内存情况下不会killed，并提供通知向用户表明处于foreground状态。
        startForeground(Constant.NOTIFY_MAPLOCATE_ID, myNotify);
    }


    @Override
    public void onDestroy() {
        // 退出时销毁定位
        DataManager.getInstance().getmLocClient().stop();
        this.timLogout();
//        DataManager.getInstance().setmLocClient(null);

        //将服务从forefround状态中移走，使得系统可以在低内存的情况下清除它
        stopForeground(true);

        if(MapMainActivity.isActive == true){
            Intent reIntent = new Intent();
            reIntent.setClass(this, MyLocationService.class);
            this.startService(reIntent);
        }else {
            restartMapMainActivity();
        }
    }

    /**
     * 检测package是否存在<br>
     * 2014-8-26 下午3:58:49
     * @return
     *
     */
    public boolean checkPackage() {
        boolean flag=false;
        packageManager = getPackageManager();
        try {
            pi = packageManager.getPackageInfo(ProcessName, 0);
            if(null!=pi){
                flag=true;
            }
        } catch (PackageManager.NameNotFoundException e) {
            flag=false;
        }

        return flag;
    }

    /**
     * 检测package是否在运行<br>
     * 2014-8-26 下午3:58:49
     * @return
     *
     */
    private boolean isBackgroundRunning() {

        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

        if (activityManager == null) return false;
        // get running tasks processes
        List<ActivityManager.RunningTaskInfo> processList = activityManager.getRunningTasks(100);
        for (ActivityManager.RunningTaskInfo info : processList) {
            if (info.topActivity.getPackageName().startsWith(ProcessName)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 初始化定位工具
     */
    public void initLocateClient(){
        if(DataManager.getInstance().getmLocClient() == null){
            // 定位初始化
            mLocClient = new LocationClient(getApplicationContext());
            mLocClient.registerLocationListener(new MyLocationListenner());
            LocationClientOption option = new LocationClientOption();
            option.setIsNeedAddress(true);
            option.setOpenGps(true); // 打开gps
            option.setCoorType("bd09ll"); // 设置坐标类型
            option.setScanSpan(Constant.scanTime);
            mLocClient.setLocOption(option);

            DataManager.getInstance().setmLocClient(mLocClient);

//            while(DataManager.getInstance().getmLocClient(). == false ){
                DataManager.getInstance().getmLocClient().start();

//            }
        }else {
            try {
                Thread.sleep(60000);
//                LocationClientOption option = new LocationClientOption();
//                option.setIsNeedAddress(true);
//                option.setOpenGps(true); // 打开gps
//                option.setCoorType("bd09ll"); // 设置坐标类型
//                option.setScanSpan(Constant.scanTime);
//                DataManager.getInstance().getmLocClient().setLocOption(option);

                DataManager.getInstance().getmLocClient().start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void requestLocation(){
        Constant.isMapTouch = false;
        Constant.isMyLocalTrace = true;
        DataManager.getInstance().setLastLocation(null);
        DataManager.getInstance().getmLocClient().requestLocation();
    }

    public String getCity() {
        return city;
    }


    /**
     * 重启看门狗服务
     */
    public void startWatchDogService(){
        if(DataManager.getInstance().getWatchDogService() == null){
            Intent reIntent = new Intent();
            reIntent.setClass(this, WatchDogService.class);
            this.startService(reIntent);
        }else {
            DataManager.getInstance().getWatchDogService().startMyLocalService();
        }
    }

    public void restartMapMainActivity(){
        Intent reIntent = new Intent();
        reIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        reIntent.setClass(this, MapMainActivity.class);
        startActivity(reIntent);
    }

    /**
     * 初始化腾讯云信监听器
     */
    public void initTimListener() {
        TIMManager.getInstance().init(getApplicationContext());

        //设置网络连接监听器，连接建立／断开时回调
        TIMManager.getInstance().setConnectionListener(new TIMConnListener() {//连接监听器
            @Override
            public void onConnected() {//连接建立
                Log.e(MyLocationService.class.getName(), "connected");
            }

            @Override
            public void onDisconnected(int code, String desc) {//连接断开
                //接口返回了错误码code和错误描述desc，可用于定位连接断开原因
                //错误码code含义请参见错误码表
                Log.e(MyLocationService.class.getName(), "disconnected");
            }

            @Override
            public void onWifiNeedAuth(String s) {

            }
        });

        //设置消息监听器，收到新消息时，通过此监听器回调
        TIMManager.getInstance().addMessageListener(new TIMMessageListener() {
            @Override
            public boolean onNewMessages(List<TIMMessage> list) {
                //消息的内容解析请参考 4.5 消息解析.

                //* 消息 *//*
                for(TIMMessage msg:list) {
                    for(int i = 0; i < msg.getElementCount(); ++i) {
                        TIMElem elem = msg.getElement(i);

                        //获取当前元素的类型
                        TIMElemType elemType = elem.getType();
                        Log.d(MyLocationService.class.getName(), "elem type: " + elemType.name());
                        if (elemType == TIMElemType.Text) {
                            //处理文本消息
                            TIMTextElem txtEl = (TIMTextElem) elem;
                            Log.i(MyLocationService.class.getName(), "receive msg: " + txtEl.getText());
                            DataManager.getInstance().getMyLocalService().setForeground("联动消息", txtEl.getText());
                        } else if (elemType == TIMElemType.Image) {
                            //处理图片消息
                        }//...处理更多消息
                    }
                }

                return true; //返回true将终止回调链，不再调用下一个新消息监听器
            }
        });

        //设置日志回调，sdk输出的日志将通过此接口回传一份副本
        //[NOTE] 请注意level定义在TIMManager中，如TIMManager.ERROR等， 并不同于Android系统定义
        TIMManager.getInstance().setLogListener(new TIMLogListener() {
            @Override
            public void log(int level, String tag, String msg) {
                //可以通过此回调将sdk的log输出到自己的日志系统中
                Log.i(tag, msg);
            }
        });

        //设置用户状态变更监听器，在回调中进行相应的处理
        TIMManager.getInstance().setUserStatusListener(new TIMUserStatusListener() {
            @Override
            public void onForceOffline() {
                //被踢下线
            }

            @Override
            public void onUserSigExpired() {
                //票据过期，需要换票后重新登录
            }
        });
    }

    /**
     * 云信服务登录
     */
    public void timLogin() {
        // identifier为用户名，userSig 为用户登录凭证

        TIMUser user = new TIMUser();
        user.setIdentifier("hrorouter1");
        user.setAccountType("9546");

        //发起登录请求
        TIMManager.getInstance().login(
                1400021692,                   //sdkAppId，由腾讯分配
                user,
                //用户帐号签名，由私钥加密获得，具体请参考文档
                "eJxljlFPgzAYRd-5FQ2vGtOWodW3OXCpDKdumZkvBGlHv5G1pOsQZ-zvIi6RxPt6Tu69nx5CyF-OFhd5UZiDdpn7qKWPbpCP-fM-WNcgstxlgRX-oGxrsDLLN07aHpIwDCnGQweE1A42cDKUNdYcOp8MnL2osn7ot2TUNVByeU2HCpQ9TOP1hD9F*spypeexYPeGnbVVlKj1ljbVjE-nE1ps2XP6NiKwi48lV*P01q6SVVqYh8Vj1TYvS-PaRuqOt5qVMD5yx4Sr4oS9T9Vg0sFOng4xygLS-RrQRto9GN0LFJOQ0AD-xPe*vG-ZdWAL",
                new TIMCallBack() {//回调接口

                    @Override
                    public void onSuccess() {//登录成功
                        Log.d(MyLocationService.class.getName(), "login success");
                    }

                    @Override
                    public void onError(int code, String desc) {//登录失败

                        //错误码code和错误描述desc，可用于定位请求失败原因
                        //错误码code含义请参见错误码表
                        Log.d(MyLocationService.class.getName(), "login failed. code: " + code + " errmsg: " + desc);
                    }
                });
    }

    /**
     * 初始化单聊会话
     */
    public void initConversation(){
        String peer = "hrorouter1";
        if(peer == null || "".equals(peer)) {
            return ;
        }
        //获取单聊会话
        conversation = TIMManager.getInstance().getConversation(
                TIMConversationType.C2C,    //会话类型：单聊
                peer);
    }

    /**
     * 退出云信
     */
    public void timLogout() {
        //登出
        TIMManager.getInstance().logout(new TIMCallBack() {
            @Override
            public void onError(int code, String desc) {

                //错误码code和错误描述desc，可用于定位请求失败原因
                //错误码code列表请参见错误码表
                Log.d(MyLocationService.class.getName(), "tim logout failed. code: " + code + " errmsg: " + desc);
            }

            @Override
            public void onSuccess() {
                Log.d(MyLocationService.class.getName(), "tim logout success");
            }
        });
    }

    /**
     * 发送消息
     * @param msg
     */
    public boolean sendTimMessage(String msg) {
        boolean isOk = false;
        if(conversation == null) {
            return isOk;
        }
        //构造一条消息
        TIMMessage tMsg = new TIMMessage();

        //添加文本内容
        TIMTextElem elem = new TIMTextElem();
        elem.setText(msg);

        //将elem添加到消息
        if(tMsg.addElement(elem) != 0) {
            Log.d(MyLocationService.class.getName(), "addElement failed");
            return isOk;
        }

        //发送消息
        conversation.sendMessage(tMsg, new TIMValueCallBack<TIMMessage>() {//发送消息回调
            @Override
            public void onError(int code, String desc) {//发送消息失败
                //错误码code和错误描述desc，可用于定位请求失败原因
                //错误码code含义请参见错误码表
                Log.d(MyLocationService.class.getName(), "send message failed. code: " + code + " errmsg: " + desc);
            }

            @Override
            public void onSuccess(TIMMessage msg) {//发送消息成功
                Log.e(MyLocationService.class.getName(), "SendMsg ok");
            }
        });

//        MessageEvent.getInstance().onNewMessage(tMsg);
//        if(!timResult.isEmpty()) {
//            if("1".equals(StringUtil.toString(timResult.get("msgState")))) {
//                isOk = true;
//            }
//        }
        return isOk;
    }

    /**
     * 发送手机短信
     * @param msg
     */
    public void sendSmsMessage(String msg) {
        SmsManager sms = SmsManager.getDefault();

        String alarmPhone = Constant.ALARM_PHONE;
        if(StringUtil.isNotBlank(alarmPhone)) {
            sms.sendTextMessage(alarmPhone, null, msg, null,null);
        }
    }

}
