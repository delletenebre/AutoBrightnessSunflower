package kg.delletenebre.autobrightnesssunflower;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;

public class EventsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        String action = intent.getAction();

        if (action.equals(Intent.ACTION_SCREEN_ON)
                || action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            final App app = App.getInstance();
            final SharedPreferences prefs = app.getPrefs();

            if (prefs.getBoolean("control_brightness_enabled", true)) {
                if (action.equals(Intent.ACTION_BOOT_COMPLETED) && !prefs.getBoolean("autostart", true)) {
                    return;
                }

                int delay = Integer.parseInt(prefs.getString("autostart_delay",
                        context.getString(R.string.pref_default_autostart_delay))) * 1000;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (prefs.getBoolean("brightness_live_update", true)) {
                            context.startService(new Intent(app, CheckBrightnessService.class));
                        } else {
                            app.updateSystemBrightness();
                        }
                    }
                }, delay);
            }
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            context.stopService(new Intent(context, CheckBrightnessService.class));
        }
    }
}
