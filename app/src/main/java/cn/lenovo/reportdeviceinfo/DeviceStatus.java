package cn.lenovo.reportdeviceinfo;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceStatus {

    private Context mContext;
    private static DeviceStatus mInstance;
    private DeviceInfo deviceInfo ;

    private boolean mIsReportPowerOn;

    private DeviceStatus(Context context){
        mContext = context;
        deviceInfo = DeviceInfo.getInstance(mContext);
    }

    public static DeviceStatus getInstance(Context context){
        if(mInstance == null){
            mInstance = new DeviceStatus(context);
        }
        return mInstance;
    }

    public void setReportPowerOn(boolean isReportPowerOn){
        mIsReportPowerOn = isReportPowerOn;
    }

    public boolean isReportPowerOn(){
        return mIsReportPowerOn;
    }

    public String toPowerOnStatus(){
        JSONObject powerOnObj = new JSONObject();
        try {
            powerOnObj.put("deviceSN", deviceInfo.getSerial());
            powerOnObj.put("deviceName", "Lenovo AR G1");
            powerOnObj.put("osVersion", "Android " + deviceInfo.getAndroidVersion());
            powerOnObj.put("coordinate", deviceInfo.beginLocatioon());
            powerOnObj.put("networkType", deviceInfo.getNetWorkType());
            powerOnObj.put("clientTime", System.currentTimeMillis() + "");
            powerOnObj.put("others", "");
            Log.d("Tmac", "coordinate = " + deviceInfo.beginLocatioon());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return powerOnObj.toString();


    }

}
