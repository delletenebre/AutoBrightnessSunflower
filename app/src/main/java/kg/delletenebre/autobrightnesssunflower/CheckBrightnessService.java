package kg.delletenebre.autobrightnesssunflower;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

public class CheckBrightnessService extends Service {
    private final String TAG = getClass().getName();

    private APP mAPP;
    private SharedPreferences _settings;
    private CommandsReceiver receiver;
    private String currentBrightnessMode;

    Handler mHandler;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            checkBrightness();
        }
    };

    @Override
    public  void onCreate() {
        super.onCreate();

        _settings = PreferenceManager.getDefaultSharedPreferences(this);
        mAPP = APP.getInstance(_settings);

        if (mAPP.getStartActivity() != null) {
            mAPP.getStartActivity().finish();
        }

        receiver = new CommandsReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, intentFilter);

        mHandler = new Handler();
        currentBrightnessMode = mAPP.getCurrentMode();

        if (mAPP.isDEBUG()) {
            Log.d(TAG, "CheckBrightnessService CREATED");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (receiver != null) {
            unregisterReceiver(receiver);
        }

        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
            mHandler = null;
        }

        if (mAPP.isDEBUG()) {
            Log.d(TAG, "CheckBrightnessService DESTROYED");
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
        try {
            String brightnessMode = mAPP.getCurrentMode();
            if (!currentBrightnessMode.equals(brightnessMode)) {
                currentBrightnessMode = brightnessMode;
                mAPP.setSystemBrightness(getApplicationContext(), null);
            }

        } catch (Exception e) {
            Log.e(TAG, "Brightness check service's runnable error: " + e.getMessage());
        }

        scheduleNext();
    }

    private void scheduleNext() {
        if (mHandler != null && _settings.getBoolean("brightness_live_update", true)
                && _settings.getBoolean("is_app_enabled", true)) {
            int delay = Integer.parseInt(
                    _settings.getString("brightness_live_update_interval", "10"));
            delay *= 60000;// delay * 60 seconds * 1000 ms
            mHandler.postDelayed(mRunnable, delay);
        } else {
            stopSelf();
        }
    }


}
