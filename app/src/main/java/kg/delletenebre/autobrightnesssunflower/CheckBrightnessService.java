package kg.delletenebre.autobrightnesssunflower;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;

public class CheckBrightnessService extends Service implements LocationListener {

    private SharedPreferences mPrefs;
    private Runnable mRunnable;
    private Handler mHandler;
    private LocationManager mLocationManager;


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
            App.getInstance().updateLocation(lastLocation);
        } else {
            mLocationManager = App.getInstance().getLocationManager();
            if (mLocationManager != null) {
                //noinspection MissingPermission
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mHandler.removeCallbacks(mRunnable);
        mHandler = null;
        mRunnable = null;
        mPrefs = null;

        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
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
        if (mHandler != null && mPrefs.getBoolean("brightness_live_update", true)) {
            int delay = Integer.parseInt(
                    mPrefs.getString("brightness_live_update_interval", "10"));
            delay *= 60000;// delay * 60 seconds * 1000 ms
            mHandler.postDelayed(mRunnable, delay);
        } else {
            stopSelf();
        }
    }

    public void onLocationChanged(Location location) {
        if (location != null) {
            App.getInstance().updateLocation(location);
            mLocationManager.removeUpdates(this);
        }
    }
    // Required functions
    public void onProviderDisabled(String arg0) {}
    public void onProviderEnabled(String arg0) {}
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}


}
