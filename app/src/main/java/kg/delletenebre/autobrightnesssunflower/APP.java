package kg.delletenebre.autobrightnesssunflower;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.dd.processbutton.iml.ActionProcessButton;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class APP extends Application {
    public static boolean DEBUG = false;
    public boolean isDEBUG() {
        return DEBUG;
    }

    public static boolean TOAST = true;
    public boolean isTOAST() {
        return TOAST;
    }

    private static APP instance = new APP();
    public static APP getInstance() {
        return instance;
    }
    public static APP getInstance(SharedPreferences settings) {
        setSettings(settings);
        DEBUG = settings.getBoolean("is_debug", false);
        TOAST = settings.getBoolean("is_toast", false);
        return instance;
    }

    private final String TAG = "Autobrightness GPS";

    private static SharedPreferences _settings;
    public static void setSettings(SharedPreferences settings) {
        _settings = settings;
    }

    public boolean timeInRange(String time, String startTime, String endTime) {
        String[] timeArray = time.split(":", -1);
        Calendar calendarTime = Calendar.getInstance();
        calendarTime.set(Calendar.HOUR, Integer.parseInt(timeArray[0]));
        calendarTime.set(Calendar.MINUTE, Integer.parseInt(timeArray[1]));

        String[] startArray = startTime.split(":", -1);
        Calendar calendarStart = Calendar.getInstance();
        calendarStart.set(Calendar.HOUR, Integer.parseInt(startArray[0]));
        calendarStart.set(Calendar.MINUTE, Integer.parseInt(startArray[1]));

        String[] endArray = endTime.split(":", -1);
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.set(Calendar.HOUR, Integer.parseInt(endArray[0]));
        calendarEnd.set(Calendar.MINUTE, Integer.parseInt(endArray[1]));

        return (calendarTime.after(calendarStart) && calendarTime.before(calendarEnd));
    }

    public String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();

        String time = String.format("%02d" , calendar.get(Calendar.HOUR_OF_DAY))
                + ":" + String.format("%02d" , calendar.get(Calendar.MINUTE));

        if (DEBUG) {
            Log.d(TAG, "Current time: " + time);
        }

        return time;
    }

    public String getCurrentDate() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());

        if (DEBUG) {
            Log.d(TAG, "Current date: " + date);
        }

        return date;
    }

    public String getCurrentMode() {
        String time = getCurrentTime();
        String sunrise = _settings.getString("sunrise", "");
        String sunset = _settings.getString("sunset", "");
        String duskStart = _settings.getString("dusk_start", "");
        String duskEnd = _settings.getString("dusk_end", "");

        if (!sunrise.isEmpty() && !sunset.isEmpty() && !duskStart.isEmpty() && !duskEnd.isEmpty()) {
            if (timeInRange(time, sunrise, sunset)) {
                return "day";
            } else if (timeInRange(time, duskStart, sunrise) || timeInRange(time, sunset, duskEnd)) {
                return "dusk";
            } else {
                return "night";
            }
        }

        return "";
    }


    public boolean hasPermissionToWriteSettings(Context context) {
        return (Build.VERSION.SDK_INT < 23 ||
                (Build.VERSION.SDK_INT >= 23 && Settings.System.canWrite(context)));
    }


    public int getSystemBrightness(Context context) {
        int brightness = -1;

        try {
            ContentResolver cResolver = context.getContentResolver();

            if (hasPermissionToWriteSettings(context)) {
                Settings.System.putInt(cResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                brightness = Settings.System.getInt(cResolver, Settings.System.SCREEN_BRIGHTNESS);

            } else {
                Intent grantIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                context.startActivity(grantIntent);
            }


        } catch (Settings.SettingNotFoundException e) {
            if (DEBUG) {
                Log.e(TAG, "Cannot access system brightness: " + e.getMessage());
            }
        }

        return brightness;
    }

    public void setSystemBrightness(Context context, Window window) {
        if (_settings.getBoolean("is_app_enabled", true)) {
            int brightness = getSystemBrightness(context);

            if (brightness > -1) {
                String mode = getCurrentMode();

                switch (mode) {
                    case "day":
                        brightness = _settings.getInt("brightness_day", 50);
                        break;
                    case "dusk":
                        brightness = _settings.getInt("brightness_dusk", 50);
                        break;
                    case "night":
                        brightness = _settings.getInt("brightness_night", 50);
                        break;
                    default:
                        brightness = -1;
                }

                if ( brightness > -1 ) {
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, (int) (2.55f * brightness));

                    if (window != null) {
                        WindowManager.LayoutParams lp = window.getAttributes();
                        lp.screenBrightness = brightness / 100f;
                        window.setAttributes(lp);
                    }

                    if (isTOAST()) {
                        Toast.makeText(context,
                                context.getString(R.string.toast_setted_brightness, brightness),
                                Toast.LENGTH_SHORT).show();
                    }

                    if (isDEBUG()) {
                        Log.d(TAG, "System brightness set to " + String.valueOf(brightness) + "% ["
                                + mode + "]");
                    }
                } else if (isDEBUG()) {
                    Log.d(TAG, "Current MODE not detected");
                }

            } else if (isDEBUG()) {
                Log.e(TAG, "Getting system brightness error");
            }
        }
    }

    public void updateSunriseSunsetSchedule(Context context,
                                            final ActionProcessButton button,
                                            final TextView txtDay,
                                            final TextView txtDusk,
                                            final TextView txtNight) {
        float lat = _settings.getFloat("lat", 181);
        float lon = _settings.getFloat("lon", 181);
        String sLat = String.valueOf(lat);
        String sLon = String.valueOf(lon);

        if (button != null) {
            button.setProgress(0);
        }

        if ((lat >= -180 && lat <= 180) && (lon >= -180 && lon <= 180)) {
            if (button != null) {
                button.setEnabled(false);
                button.setProgress(1);
            }

            final AQuery AQ = new AQuery(context);
            AQ.ajax("http://api.sunrise-sunset.org/json?lat=" + sLat + "&lng=" + sLon + "&formatted=0",
                    JSONObject.class, new AjaxCallback<JSONObject>() {

                        @Override
                        public void callback(String url, JSONObject json, AjaxStatus status) {
                            if (button != null) {
                                button.setEnabled(true);
                            }

                            if (json != null) {
                                if (button != null) {
                                    button.setProgress(100);
                                }

                                String sStatus = json.optString("status");
                                if (sStatus.equals("OK")) {
                                    try {
                                        JSONObject result = json.getJSONObject("results");
                                        String  sSunrise = result.optString("sunrise"),
                                                sSunset = result.optString("sunset"),
                                                sDuskStart = result.optString("civil_twilight_begin"),
                                                sDuskEnd = result.optString("civil_twilight_end"),
                                                sNauticalTwB = result.optString("nautical_twilight_begin"),
                                                sNauticalTwE = result.optString("nautical_twilight_end"),
                                                sAstroTwB = result.optString("astronomical_twilight_begin"),
                                                sAstroTwE = result.optString("astronomical_twilight_end");

                                        SimpleDateFormat hhmmFormat = new SimpleDateFormat("HH:mm", Locale.ROOT);
                                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT);
                                        dateFormat.setTimeZone(TimeZone.getDefault());
                                        try {
                                            Date dateSunrise = dateFormat.parse(sSunrise),
                                                    dateSunset = dateFormat.parse(sSunset),
                                                    dateCivilTwB = dateFormat.parse(sDuskStart),
                                                    dateCivilTwE = dateFormat.parse(sDuskEnd),
                                                    dateNauticalTwB = dateFormat.parse(sNauticalTwB),
                                                    dateNauticalTwE = dateFormat.parse(sNauticalTwE),
                                                    dateAstroTwB = dateFormat.parse(sAstroTwB),
                                                    dateAstroTwE = dateFormat.parse(sAstroTwE);

                                            sSunrise = hhmmFormat.format(dateSunrise);
                                            sSunset = hhmmFormat.format(dateSunset);
                                            sDuskStart = hhmmFormat.format(dateCivilTwB);
                                            sDuskEnd = hhmmFormat.format(dateCivilTwE);
                                            sNauticalTwB = hhmmFormat.format(dateNauticalTwB);
                                            sNauticalTwE = hhmmFormat.format(dateNauticalTwE);
                                            sAstroTwB = hhmmFormat.format(dateAstroTwB);
                                            sAstroTwE = hhmmFormat.format(dateAstroTwE);

                                            SharedPreferences.Editor _settingEditor = _settings.edit();
                                            _settingEditor.putString("sunrise", sSunrise);
                                            _settingEditor.putString("sunset", sSunset);
                                            _settingEditor.putString("dusk_start", sDuskStart);
                                            _settingEditor.putString("dusk_end", sDuskEnd);
                                            _settingEditor.putString("last_update_date", getCurrentDate());

                                            _settingEditor.apply();

                                            if (isTOAST()) {
                                                Toast.makeText(AQ.getContext(),
                                                        R.string.toast_sun_schedule_updated,
                                                        Toast.LENGTH_SHORT).show();
                                            }

                                            if (txtDay != null && txtDusk != null && txtNight != null) {
                                                txtDay.setText(
                                                        AQ.getContext().getString(R.string.time_placeholder,
                                                                sSunrise, sSunset));
                                                txtDusk.setText(
                                                        AQ.getContext().getString(R.string.times_placeholder,
                                                                sDuskStart, sSunrise, sSunset, sDuskEnd));
                                                txtNight.setText(
                                                        AQ.getContext().getString(R.string.time_placeholder,
                                                                sDuskEnd, sDuskStart));
                                            }

                                            if (isDEBUG()) {
                                                Log.d(TAG, "Sunrise: " + sSunrise);
                                                Log.d(TAG, "Sunset: " + sSunset);
                                                Log.d(TAG, "Civil twilight begin: " + sDuskStart);
                                                Log.d(TAG, "Civil twilight end: " + sDuskEnd);
                                                Log.d(TAG, "Nautical twilight begin: " + sNauticalTwB);
                                                Log.d(TAG, "Nautical twilight end: " + sNauticalTwE);
                                                Log.d(TAG, "Astronomical twilight begin: " + sAstroTwB);
                                                Log.d(TAG, "Astronomical twilight end: " + sAstroTwE);
                                            }
                                        } catch (Exception e) {
                                            if (isDEBUG()) {
                                                Log.e(TAG, "Time parse error: " + e.getMessage());
                                            }

                                            if (button != null) {
                                                button.setProgress(-1);
                                            }
                                        }

                                    } catch (Exception e) {
                                        if (isDEBUG()) {
                                            Log.e(TAG, "JSON parse error: " + e.getMessage());
                                        }

                                        if (button != null) {
                                            button.setProgress(-1);
                                        }
                                    }

                                } else {
                                    if (isDEBUG()) {
                                        Log.e(TAG, "http://sunrise-sunset.org/ error: " + json.toString());
                                    }

                                    if (button != null) {
                                        button.setProgress(-1);
                                    }
                                }

                            } else {
                                if (isDEBUG()) {
                                    Log.e(TAG, "http://sunrise-sunset.org/ error: "
                                            + "[URL: " + url + "] [Code: "
                                            + String.valueOf(status.getCode()) + "]");
                                }

                                if (button != null) {
                                    button.setProgress(-1);
                                }

                                if (isTOAST()) {
                                    Toast.makeText(AQ.getContext(),
                                            R.string.toast_network_error,
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });
        } else {
            if (isDEBUG()) {
                Log.e(TAG, "Coordinate(s) error: " + "[lat] " + sLat + ", " + "[lon] " + sLon);
            }

        }
    }

    public void updateSunriseSunsetSchedule(Context context) {
        updateSunriseSunsetSchedule(context, null, null, null, null);
    }

    public boolean isTimeToUpdateSunSchedule() {
        boolean result = false;
        if (_settings.getBoolean("is_sun_schedule_update", true)) {
            String sLastUpdateDate = _settings.getString("last_update_date", "");

            if (sLastUpdateDate.isEmpty()) {
                result = true;
            } else {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);
                try {
                    Date lastUpdateDate = formatter.parse(sLastUpdateDate);
                    Calendar calendarLastUpdateDay = Calendar.getInstance();
                    calendarLastUpdateDay.setTime(lastUpdateDate);

                    Calendar today = Calendar.getInstance();

                    long diff = (today.getTimeInMillis() - calendarLastUpdateDay.getTimeInMillis())
                            / (24 * 60 * 60 * 1000);
                    int interval = Integer.parseInt(
                            _settings.getString("interval_sun_schedule_update", "7"));

                    if (DEBUG) {
                        Log.d(TAG, "Days from last update sun schedule: " + String.valueOf(diff));
                    }

                    result = (diff >= interval);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}
