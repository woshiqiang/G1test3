package cn.lenovo.reportdeviceinfo;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import cn.lenovo.reportdeviceinfo.httpRequest.OkHttpClientUtil;
import cn.lenovo.reportdeviceinfo.httpRequest.UrlConstant;
import cn.lenovo.reportdeviceinfo.network.NetState;
import okhttp3.Request;

public class DeviceService extends Service {

    public static String MSG_CMD = "MSG_CMD";
    public final static int MSG_POWER_ON = 1;
    public final static int MSG_APP = 2;

    private OkHttpClientUtil okHttpClientUtil;
    private DeviceInfo deviceInfo;
    private NetState mNetState;

    private DeviceStatus mDeviceStatus;

    private HandlerThread mWorkThread = null;
    private Handler mWorkHandler;
    private AppManager appManager;
    private DeviceStatus deviceStatus;
    // 定位
    private LocationService locationService;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("DeviceService", " 服务启动------");
//        Toast.makeText(this, "onCreate------", Toast.LENGTH_SHORT).show();

        okHttpClientUtil = OkHttpClientUtil.getInstance();
        mNetState = NetState.getInstance();
        mDeviceStatus = DeviceStatus.getInstance(this);
        deviceInfo = DeviceInfo.getInstance(this);
        deviceStatus = DeviceStatus.getInstance(this);
        mWorkThread = new HandlerThread("DSWorkThread");
        mWorkThread.start();
        mWorkHandler = new SFServiceHandler(mWorkThread.getLooper());
        appManager = new AppManager(this, mWorkHandler);

        initLocation();

        if (mNetState.hasNetWorkConnection(this)) {
            reportPowerOff();
            reportErrorLog("error");
        }

    }

    private void initLocation() {
        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用，可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService = new LocationService(getApplicationContext());
        //注册监听
        locationService.registerListener(mListener);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("DeviceService", "onStartCommand------");
//        Toast.makeText(this, "onStartCommand------", Toast.LENGTH_SHORT).show();
        if (intent == null) {
            int cmdId = intent.getIntExtra(MSG_CMD, -1);
            Message.obtain(mWorkHandler, cmdId, intent).sendToTarget();
        }

        return super.onStartCommand(intent, flags, startId);
    }


    private class SFServiceHandler extends Handler {
        private SFServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            int cmdId = msg.what;
            switch (cmdId) {
                case MSG_POWER_ON:
                    Log.d("SFServiceHandler", "开始定位");
                    locationService.start();//开始定位
                    break;
                case MSG_APP:
                    String packageName = (String) msg.obj;
                    reportOpenApp(packageName);
                    break;
            }
            super.handleMessage(msg);

        }
    }


    private void reportPowerOnInfo(String Coordinate) {

        HashMap<String, String> map = new HashMap<>();
        map.put("DeviceSN", deviceInfo.getSerial());
        map.put("DeviceName", "tutu");
        map.put("OSVersion", "Android " + deviceInfo.getAndroidVersion());
        map.put("Coordinate", Coordinate);
        map.put("NetworkType", deviceInfo.getNetWorkType());
        map.put("ClientTime", System.currentTimeMillis() + "");
        map.put("others", "");

        okHttpClientUtil._postAsyn(UrlConstant.POWER_ON, new OkHttpClientUtil.ResultCallback<String>() {
            @Override
            public void onError(Request request, final Exception e) {
                Log.d("tmac", "onError : " + e.getMessage());

            }

            @Override
            public void onResponse(final String response) {
                Log.d("tmac", "onResponse : " + response.toString());
                mDeviceStatus.setReportPowerOn(true);
            }
        }, map);
    }



    private void reportPowerOff() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceSN", deviceInfo.getSerial());
            jsonObject.put("clientTime", System.currentTimeMillis() + "");
            jsonObject.put("others", "poweroff");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        okHttpClientUtil._postAsyn(UrlConstant.POWER_OFF, new OkHttpClientUtil.ResultCallback<String>() {
            @Override
            public void onError(Request request, final Exception e) {
                Log.d("tmac", "onError : " + e.getMessage());
            }

            @Override
            public void onResponse(final String response) {
                Log.d("tmac", "onResponse : " + response.toString());
            }
        }, jsonObject.toString());
    }

    private void reportOpenApp(String packageName) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceSN", deviceInfo.getSerial());
            jsonObject.put("appName", packageName);
            jsonObject.put("clientTime", "" + System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        okHttpClientUtil._postAsyn(UrlConstant.OPEN_APP, new OkHttpClientUtil.ResultCallback<String>() {
            @Override
            public void onError(Request request, final Exception e) {
                Log.d("tmac", "onError : " + e.getMessage());
            }

            @Override
            public void onResponse(final String response) {
                Log.d("tmac", "onResponse : " + response.toString());
            }
        }, jsonObject.toString());

    }

    private void reportErrorLog(String error) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("deviceSN", deviceInfo.getSerial());
            jsonObject.put("error", error);
            jsonObject.put("clientTime", System.currentTimeMillis() + "");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        okHttpClientUtil._postAsyn(UrlConstant.ERROR_LOG, new OkHttpClientUtil.ResultCallback<String>() {
            @Override
            public void onError(Request request, final Exception e) {
                Log.d("tmac", "onError : " + e.getMessage());
            }

            @Override
            public void onResponse(final String response) {
                Log.d("tmac", "onResponse : " + response.toString());
            }
        }, jsonObject.toString());
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mWorkThread) mWorkThread.quit();
    }

    /*****
     *
     * 定位结果回调，重写onReceiveLocation方法
     *
     */
    private BDAbstractLocationListener mListener = new BDAbstractLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {
                double latitude = location.getLatitude();// 纬度
                double longitude = location.getLongitude();// 经度
                //定位成功，上传信息
                reportPowerOnInfo(longitude + "," + latitude);
                locationService.stop();
            }
        }

    };


}
