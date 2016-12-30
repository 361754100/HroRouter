package com.hro.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Projection;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.sug.OnGetSuggestionResultListener;
import com.baidu.mapapi.search.sug.SuggestionResult;
import com.baidu.mapapi.search.sug.SuggestionSearch;
import com.baidu.mapapi.search.sug.SuggestionSearchOption;
import com.hro.router.data.DataManager;
import com.hro.router.service.MyLocationService;
import com.hro.router.util.Constant;
import com.hro.router.util.PoiOverlay;
import com.hro.router.util.SystemUiHider;
import com.hro.router.util.Utils;
import com.iflytek.sunflower.FlowerCollector;

import java.util.List;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class MapMainActivity extends Activity {

    private MapView mMapView = null;
    private BaiduMap mBaiduMap = null;
    private ImageButton myLocBtn = null;
    private EditText mapKeywords = null;

    private PoiSearch mPoiSearch = null;
    private SuggestionSearch mSuggestionSearch = null;


    private String city	="";
    private long exitTime = 0;  //用于判断是否退出程序
    public static boolean isActive = false;

    void doBindService() {
        // 建立service连接。因为我们知道程序会运行在本地里，因此使用显示的类名来实现service
        // （但是不支持跟其他程序交互）
        // 两种传递，一种是在manifest里写好intent-filter的action，一种是显示传递

        //如果用这种方法将会调用onStartCommand方法
        startService(new Intent(MapMainActivity.this, MyLocationService.class));
        DataManager.getInstance().setBMap(mBaiduMap);
    }

    void doUnbindService() {
        stopService(new Intent(MapMainActivity.this, MyLocationService.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isActive = true;

        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_map_main);

        myLocBtn = (ImageButton) findViewById(R.id.myLocation_button);
        myLocBtn.setOnClickListener(new MyLocBtnClickListener());

        mapKeywords = (EditText)findViewById(R.id.map_keywords);
        mapKeywords.setOnKeyListener(new OnKeyListener(){
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_ENTER){
                    Constant.isMyLocalTrace = false;

                    InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(imm.isActive()){
                        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0 );
                    }

                    String keywords = mapKeywords.getText().toString();
                    if(!"".equals(keywords.trim())){
						String[] words = keywords.split(" ");
						String place = "";
                        if(words.length == 1) {
                            mSuggestionSearch.requestSuggestion(new SuggestionSearchOption().keyword(keywords).city(city));
                        }else if(words.length > 1 ){
							for(int i = 1; i<words.length ;i++){
								place += words[i]+" ";
							}
                            mPoiSearch.searchInCity((new PoiCitySearchOption())
                                    .city(words[0])
                                    .keyword(keywords)
                                    .pageNum(10));
						}

                    }
                    return true;
                }
                return false;
            }
        });

        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);

        mPoiSearch = PoiSearch.newInstance();
        // 初始化建议搜索模块，注册建议搜索事件监听
        mSuggestionSearch = SuggestionSearch.newInstance();

        mPoiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener(){
            public void onGetPoiResult(PoiResult poiResult){
                //获取POI检索结果
                if (poiResult == null
                        || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {// 没有找到检索结果
                    Toast.makeText(MapMainActivity.this, "未找到结果",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (poiResult.error == SearchResult.ERRORNO.NO_ERROR) {// 检索结果正常返回
                    mBaiduMap.clear();
                    MyPoiOverlay poiOverlay = new MyPoiOverlay(mBaiduMap);
                    poiOverlay.setData(poiResult);// 设置POI数据
                    mBaiduMap.setOnMarkerClickListener(poiOverlay);
                    poiOverlay.addToMap();// 将所有的overlay添加到地图上
                    poiOverlay.zoomToSpan();
                    //
                    Toast.makeText(
                            MapMainActivity.this,
                            "总共查到" + poiResult.getTotalPoiNum() + "个兴趣点, 分为"
                                    + poiResult.getTotalPageNum() + "页", Toast.LENGTH_SHORT).show();

                }
            }
            public void onGetPoiDetailResult(PoiDetailResult result){
                //获取Place详情页检索结果
            }
            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult arg0) {

            }
        });

        BaiduMap.OnMapStatusChangeListener onMapStatusChangeListener = new BaiduMap.OnMapStatusChangeListener() {
            LatLng startLng, finishLng;

            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {
                startLng = mapStatus.target;
            }

            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {
                if( Constant.MAP_DEFAULT_ZOOM != mapStatus.zoom){
                    Constant.isMyLocalTrace = false;
                    return;
                }

                // 滑动搜索
                finishLng = mapStatus.target;
                if (startLng.latitude != finishLng.latitude
                        || startLng.longitude != finishLng.longitude) {
                    Projection ject = mBaiduMap.getProjection();
                    Point startPoint = ject.toScreenLocation(startLng);
                    Point finishPoint = ject.toScreenLocation(finishLng);
                    double x = Math.abs(finishPoint.x - startPoint.x);
                    double y = Math.abs(finishPoint.y - startPoint.y);
                    //在这处理滑动事件逻辑
                    if ( Constant.isMapTouch == true && ( x > Constant.MAP_DRAG_DIST || y > Constant.MAP_DRAG_DIST) ) {
                        Constant.isMyLocalTrace = false;
                    }
                }
            }

            @Override
            public void onMapStatusChange(MapStatus mapStatus) {
            }
        };
        mBaiduMap.setOnMapStatusChangeListener(onMapStatusChangeListener);

        BaiduMap.OnMapTouchListener mapTouchListener = new BaiduMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                Constant.isMapTouch = true;
            }
        };
        mBaiduMap.setOnMapTouchListener(mapTouchListener);

        mSuggestionSearch.setOnGetSuggestionResultListener(new OnGetSuggestionResultListener() {
            @Override
            public void onGetSuggestionResult(SuggestionResult suggestionResult) {
                List<SuggestionResult.SuggestionInfo> suggests = suggestionResult.getAllSuggestions();
                if(suggests != null && suggests.size() > 0){
//
//                  suggests.get(0);
//                    PoiResult poiResult = new PoiResult();
//                    MyPoiOverlay poiOverlay = new MyPoiOverlay(mBaiduMap);
//
//                    poiOverlay.setData(poiResult);// 设置POI数据
//                    mBaiduMap.setOnMarkerClickListener(poiOverlay);
//                    poiOverlay.addToMap();// 将所有的overlay添加到地图上
//                    poiOverlay.zoomToSpan();
                }
            }
        });

        doBindService();
//        this.initBaiduPush();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
            if((System.currentTimeMillis()-exitTime) > 2000){
                Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();

        FlowerCollector.onResume(this);
    }

    @Override
    protected void onPause() {

        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();

        FlowerCollector.onPause(this);

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        isActive = false;
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView.onDestroy();
        mMapView = null;

        mPoiSearch.destroy();
        mSuggestionSearch.destroy();

        doUnbindService();
        super.onDestroy();
    }


    /**
     * 定位按钮点击事件
     * @author user
     *
     */
    public class MyLocBtnClickListener implements OnClickListener{
        @Override
        public void onClick(View arg0) {
            if(DataManager.getInstance().getMyLocalService()  != null){
                DataManager.getInstance().getMyLocalService().requestLocation();
            }
            /*
            //传入服务， parse（）解析号码
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:10086"));
            //通知activtity处理传入的call服务
            MapMainActivity.this.startActivity(intent);
            */
        }

    }

    class MyPoiOverlay extends PoiOverlay {
        public MyPoiOverlay(BaiduMap arg0) {
            super(arg0);
        }
        @Override
        public boolean onPoiClick(int arg0) {
            super.onPoiClick(arg0);
            PoiInfo poiInfo = getPoiResult().getAllPoi().get(arg0);
            // 检索poi详细信息
            mPoiSearch.searchPoiDetail(new PoiDetailSearchOption()
                    .poiUid(poiInfo.uid));
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, Menu.FIRST+1, 0, "设置");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        //得到被点击的item的itemId
        switch(item.getItemId()) {
            case Menu.FIRST+1: //对应的ID就是在add方法中所设定的Id
                Intent intent =new Intent(MapMainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    private void initBaiduPush(){
        // Push: 以apikey的方式登录，一般放在主Activity的onCreate中。
        // 这里把apikey存放于manifest文件中，只是一种存放方式，
        // 您可以用自定义常量等其它方式实现，来替换参数中的Utils.getMetaValue(PushDemoActivity.this,
        // "api_key")
        //！！ 请将AndroidManifest.xml 137行 api_key 字段值修改为自己的 api_key 方可使用 ！！
        //！！ ATTENTION：You need to modify the value of api_key to your own at row 137 in AndroidManifest.xml to use this Demo !!
        // 启动百度push
        PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY,
                Utils.getMetaValue(MapMainActivity.this, "com.baidu.android.pushservice.API_KEY"));
        // Push: 如果想基于地理位置推送，可以打开支持地理位置的推送的开关
        //PushManager.enableLbs(getApplicationContext());

        // Push: 设置自定义的通知样式，具体API介绍见用户手册，如果想使用系统默认的可以不加这段代码
        // 请在通知推送界面中，高级设置->通知栏样式->自定义样式，选中并且填写值：1，
        // 与下方代码中 PushManager.setNotificationBuilder(this, 1, cBuilder)中的第二个参数对应
        /*
        CustomPushNotificationBuilder cBuilder = new CustomPushNotificationBuilder(
                resource.getIdentifier(
                        "notification_custom_builder", "layout", pkgName),
                resource.getIdentifier("notification_icon", "id", pkgName),
                resource.getIdentifier("notification_title", "id", pkgName),
                resource.getIdentifier("notification_text", "id", pkgName));
        cBuilder.setNotificationFlags(Notification.FLAG_AUTO_CANCEL);
        cBuilder.setNotificationDefaults(Notification.DEFAULT_VIBRATE);
        cBuilder.setStatusbarIcon(this.getApplicationInfo().icon);
        cBuilder.setLayoutDrawable(resource.getIdentifier(
                "simple_notification_icon", "drawable", pkgName));
        cBuilder.setNotificationSound(Uri.withAppendedPath(
                Audio.Media.INTERNAL_CONTENT_URI, "6").toString());
        // 推送高级设置，通知栏样式设置为下面的ID
        PushManager.setNotificationBuilder(this, 1, cBuilder);
        */
    }

}
