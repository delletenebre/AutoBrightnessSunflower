package kg.delletenebre.autobrightnesssunflower;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;

public class HiddenActivity extends Activity {
    private final String TAG = getClass().getName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hidden);

        SharedPreferences _settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (_settings.getBoolean("is_app_enabled", true)) {
            final APP mAPP = APP.getInstance(_settings);

            mAPP.setSystemBrightness(this, null);

            final Context context = this;

            if (_settings.getBoolean("is_sun_schedule_update", true)) {
                int delay = Integer.parseInt(_settings.getString("is_sun_schedule_update_delay", "10"))
                        * 1000;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mAPP.isTimeToUpdateSunSchedule()) {
                            mAPP.updateSunriseSunsetSchedule(context);
                        }
                    }
                }, delay);
            }

            if (_settings.getBoolean("brightness_live_update", true)) {
                startService(new Intent(this, CheckBrightnessService.class));
            }
        }

        finish();
    }
}
