package kg.delletenebre.autobrightnesssunflower;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;

public class CheckBrightnessService extends Service {

    private SharedPreferences mPrefs;
    private Runnable mRunnable;
    private Handler mHandler;


    @Override
    public  void onCreate() {
        super.onCreate();

        mPrefs = App.getInstance().getPrefs();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                checkBrightness();
            }
        };
        mHandler = new Handler();

        android.location.Location lastLocation = App.getInstance().getLastKnownLocation();
        if (lastLocation != null) {
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putFloat("lat", (float) lastLocation.getLatitude());
            prefsEditor.putFloat("lon", (float) lastLocation.getLongitude());
            prefsEditor.apply();

            Intent intent = new Intent(App.ACTION_LOCATION_UPDATE);
            intent.putExtra("lat", lastLocation.getLatitude());
            intent.putExtra("lon", lastLocation.getLongitude());
            sendBroadcast(intent);

            App.getInstance().updateSystemBrightness();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mHandler.removeCallbacks(mRunnable);
        mHandler = null;
        mRunnable = null;
        mPrefs = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkBrightness();

        return START_STICKY;
    }

    private void checkBrightness() {
        App.getInstance().updateSystemBrightness();
        scheduleNext();
    }

    private void scheduleNext() {
        if (mHandler != null && mPrefs.getBoolean("brightness_live_update", true)
                && mPrefs.getBoolean("is_app_enabled", true)) {
            int delay = Integer.parseInt(
                    mPrefs.getString("brightness_live_update_interval", "10"));
            delay *= 60000;// delay * 60 seconds * 1000 ms
            mHandler.postDelayed(mRunnable, delay);
        } else {
            stopSelf();
        }
    }


}
