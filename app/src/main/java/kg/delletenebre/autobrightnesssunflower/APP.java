package kg.delletenebre.autobrightnesssunflower;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.LocationManager;
import android.location.OnNmeaMessageListener;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;

import java.io.DataOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class App extends Application {
    private static App sInstance;
    public static App getInstance() {
        return sInstance;
    }

    public static final String TAG = "Autobrightness GPS";
    public static final String ACTION_LOCATION_UPDATE = "kg.delletenebre.sunflower.LOCATION_UPDATE";
    public static final String ACTION_SCHEDULE_UPDATE = "kg.delletenebre.sunflower.SCHEDULE_UPDATE";


    public boolean mDebugState = false;
    private SharedPreferences mPrefs;
    private LocationManager mLocationManager;
    private OnNmeaMessageListener mNmeaMessageListener;
    private GpsStatus.NmeaListener mNmeaListener;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        setDebugEnabled(mPrefs.getBoolean("is_debug", false));

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new EventsReceiver(), intentFilter);

        mLocationManager = (LocationManager)
                getSystemService(Context.LOCATION_SERVICE);

        if (mPrefs.getBoolean("gps_update_time", false) && App.getInstance().hasGpsPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mNmeaMessageListener = new OnNmeaMessageListener() {
                    @Override
                    public void onNmeaMessage(String nmea, long timestamp) {
                        parseNmea(nmea);
                    }
                };
                //noinspection MissingPermission
                mLocationManager.addNmeaListener(mNmeaMessageListener);
            } else {
                mNmeaListener = new GpsStatus.NmeaListener() {
                    public void onNmeaReceived(long timestamp, String nmea) {
                        parseNmea(nmea);
                    }
                };
                //noinspection MissingPermission
                mLocationManager.addNmeaListener(mNmeaListener);
            }
        }
    }

    public SharedPreferences getPrefs() {
        return mPrefs;
    }

    public boolean isDebugEnabled() {
        return mDebugState;
    }

    public void setDebugEnabled(boolean state) {
        mDebugState = state;
    }

    public android.location.Location getLastKnownLocation() {
        android.location.Location location = null;
        if (mLocationManager != null && hasGpsPermission()) {
            List<String> matchingProviders = mLocationManager.getAllProviders();
            for (String provider: matchingProviders) {
                //noinspection MissingPermission
                location = mLocationManager.getLastKnownLocation(provider);
                if (location != null) {
                    Log.d(TAG, "saved lat:" + location.getLatitude());
                    Log.d(TAG, "saved lon:" + location.getLongitude());
                    break;
                }
            }
        }

        return location;
    }

    private void stopNmeaListeners() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && mNmeaMessageListener != null) {
            mLocationManager.removeNmeaListener(mNmeaMessageListener);
        } else if (mNmeaListener != null) {
            mLocationManager.removeNmeaListener(mNmeaListener);
        }
    }

    public boolean timeInRange(String time, String startTime, String endTime) {
        String[] timeArray = time.split(":", -1);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, Integer.parseInt(timeArray[0]));
        calendar.set(Calendar.MINUTE, Integer.parseInt(timeArray[1]));

        String[] startArray = startTime.split(":", -1);
        Calendar calendarStart = Calendar.getInstance();
        calendarStart.set(Calendar.HOUR, Integer.parseInt(startArray[0]));
        calendarStart.set(Calendar.MINUTE, Integer.parseInt(startArray[1]));

        String[] endArray = endTime.split(":", -1);
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.set(Calendar.HOUR, Integer.parseInt(endArray[0]));
        calendarEnd.set(Calendar.MINUTE, Integer.parseInt(endArray[1]));

        return (calendar.after(calendarStart) && calendar.before(calendarEnd));
    }

    public String getCurrentTime() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm", Locale.ROOT);
        return df.format(Calendar.getInstance().getTime());
    }

    public String getCurrentMode() {
        String time = getCurrentTime();
        String sunrise = mPrefs.getString("sunrise", "");
        String sunset = mPrefs.getString("sunset", "");
        String duskStart = mPrefs.getString("dusk_start", "");
        String duskEnd = mPrefs.getString("dusk_end", "");

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


    public int getSystemBrightness() {
        int brightness = -1;

        if (hasPermissionToWriteSettings(this)) {
            try {
                int mode = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE);
                if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                }

                brightness = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);

                if (isDebugEnabled()) {
                    Log.d(TAG, "Current brightness: " + brightness);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, R.string.toast_need_permissions, Toast.LENGTH_LONG).show();

            if (Build.VERSION.SDK_INT >= 23) {
                Intent grantIntent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                grantIntent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivity(grantIntent);
            }
        }

        return brightness;
    }

    public void updateSystemBrightness() {
        updateSunriseSunsetSchedule();

        if (mPrefs.getBoolean("is_app_enabled", true)) {
            int currentBrightness = getSystemBrightness();

            if (currentBrightness > -1) {
                String mode = getCurrentMode();
                int brightnessPercent = 50;
                switch (mode) {
                    case "day":
                        brightnessPercent = mPrefs.getInt("brightness_day", brightnessPercent);
                        break;
                    case "dusk":
                        brightnessPercent = mPrefs.getInt("brightness_dusk", brightnessPercent);
                        break;
                    case "night":
                        brightnessPercent = mPrefs.getInt("brightness_night", brightnessPercent);
                        break;
                }
                int brightness = (int) (2.55 * brightnessPercent);

                if (currentBrightness != brightness) {
                    Settings.System.putInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, brightness);
                }

                if (isDebugEnabled()) {
                    Toast.makeText(this,
                            getString(R.string.toast_setted_brightness, brightness, brightnessPercent),
                            Toast.LENGTH_SHORT).show();
                    Log.d(TAG, String.format(Locale.ROOT,
                            "System brightness set to %1$d (%2$d%%) [%3$s]",
                            brightness, brightnessPercent, mode));
                }
            } else if (isDebugEnabled()) {
                Log.e(TAG, "Getting system brightness error");
            }
        }
    }


    public void updateSunriseSunsetSchedule() {
        float lat = mPrefs.getFloat("lat", 181);
        float lon = mPrefs.getFloat("lon", 181);

        if (coordinatesInRange(lat, lon)) {
            SunriseSunsetCalculator ssCalculator = new SunriseSunsetCalculator(
                    new Location(String.valueOf(lat), String.valueOf(lon)),
                    (TimeZone.getDefault()).getID());

            String civilSunrise = ssCalculator.getCivilSunriseForDate(Calendar.getInstance());
            String officialSunrise = ssCalculator.getOfficialSunriseForDate(Calendar.getInstance());
            String officialSunset = ssCalculator.getOfficialSunsetForDate(Calendar.getInstance());
            String civilSunset = ssCalculator.getCivilSunsetForDate(Calendar.getInstance());

            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.putString("sunrise", officialSunrise);
            prefsEditor.putString("sunset", officialSunset);
            prefsEditor.putString("dusk_start", civilSunrise);
            prefsEditor.putString("dusk_end", civilSunset);
            prefsEditor.apply();

            Intent intent = new Intent(App.ACTION_SCHEDULE_UPDATE);
            intent.putExtra("sunrise", officialSunrise);
            intent.putExtra("sunset", officialSunset);
            intent.putExtra("dusk_start", civilSunrise);
            intent.putExtra("dusk_end", civilSunset);
            sendBroadcast(intent);
        }
    }

    public boolean hasGpsPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
    }

    public boolean coordinatesInRange(double lat, double lon) {
        return (lat >= -180 && lat <= 180) && (lon >= -180 && lon <= 180);
    }

    private void parseNmea(String nmeaString) {
        long timestamp = getTimestampFromGPRMC(nmeaString);
        if (timestamp > 0) {
            setTime(timestamp / 1000);
            updateSystemBrightness();
            stopNmeaListeners();
        }
    }

    private long getTimestampFromGPRMC(String nmeaSentence) {
        if (nmeaSentence.startsWith("$GPRMC")) {
            try {
                String[] nmea = nmeaSentence.split(",");
                if (nmea.length > 9) {
                    if (nmea[1].contains(".")) {
                        nmea[1] = nmea[1].substring(0, nmea[1].indexOf("."));
                    }

                    SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss", Locale.US);
                    SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("ddMMyy", Locale.US);
                    TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
                    DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));

                    long time = TIME_FORMAT.parse(nmea[1]).getTime();
                    long date = DATE_FORMAT.parse(nmea[9]).getTime();

                    return (date + time);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return -1;
    }

    private void setTime(final long time) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes(String.format(Locale.ROOT, "date -u %d;\n", time));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
