package cn.lenovo.reportdeviceinfo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AppManager {

    private static final String TAG = "SF-AppManager";

    private Context mContext;
    private Handler mHandler;
    private Timer mAppTimer;
    private CurrentAppTimerTask mAppTimerTask;
    private PackageManager pm;


    public AppManager(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        pm = mContext.getPackageManager();
        getCurrentApplicationInfo();
    }

    private ActivityManager manager;
    private String mCurrentPkgName;
    private long startAppTime = 0;


    /**
     * get current app info
     */
    private void getCurrentApplicationInfo() {
        manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        mAppTimerTask = new CurrentAppTimerTask();
        mAppTimer = new Timer();
        mAppTimer.schedule(mAppTimerTask, 0, 1000);
    }

    class CurrentAppTimerTask extends TimerTask {

        @Override
        public void run() {
            List<ActivityManager.RunningTaskInfo> runningTask = manager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo runningTaskInfo = runningTask.get(0);
            String packageName = runningTaskInfo.baseActivity.getPackageName();
            Log.d(TAG, "packageName = " + packageName);
            if (mCurrentPkgName == null) {
                mCurrentPkgName = packageName;
                startAppTime = System.currentTimeMillis();
                Log.d(TAG, "startAppTime = " + startAppTime);
            } else if (packageName.equals(mCurrentPkgName)) {
                // report cloud
                long foregroundTime = System.currentTimeMillis() - startAppTime;
                String appName = getProgramNameByPackageName(mCurrentPkgName);
                //Log.d(TAG, "report current app name = " + appName + "../ ..pkgName" + mCurrentPkgName);
                //mAppStatus.setAppForeInfo(appName, mCurrentPkgName, foregroundTime);

                Message message = new Message();
                message.what = DeviceService.MSG_APP;
                message.obj = packageName;
                mHandler.sendMessage(message);
            } else {
                startAppTime = System.currentTimeMillis();
                mCurrentPkgName = packageName;
            }

        }

    }

    private String getProgramNameByPackageName(String packageName) {
        String name = null;
        try {
            name = pm.getApplicationLabel(
                    pm.getApplicationInfo(packageName,
                            PackageManager.GET_META_DATA)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return name;
    }
}
