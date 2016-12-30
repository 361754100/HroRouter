package com.hro.router.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.baidu.location.f;
import com.hro.router.data.DataManager;

/**
 * Created by Mr Mo on 2016/11/25.
 */

public class WatchDogService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DataManager.getInstance().setWatchDogService(this);
//        startMyLocalService();
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        DataManager.getInstance().setWatchDogService(null);
        startMyLocalService();
        super.onDestroy();
    }

    /**
     * 重启位置服务
     */
    public void startMyLocalService(){
        if(DataManager.getInstance().getMyLocalService() == null){
            Intent reIntent = new Intent();
            reIntent.setClass(this, MyLocationService.class);
            this.startService(reIntent);

//            Intent reIntent2 = new Intent();
//            reIntent2.setClass(getApplicationContext(), f.class);
//            this.startService(reIntent2);
        }else {
            DataManager.getInstance().getMyLocalService().startWatchDogService();
        }
    }

}
