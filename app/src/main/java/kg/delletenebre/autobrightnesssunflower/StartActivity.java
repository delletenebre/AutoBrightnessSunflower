package kg.delletenebre.autobrightnesssunflower;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class StartActivity extends AppCompatActivity {
    private final Context mContext = this;

    private SharedPreferences mPrefs;
    private App mApp;
    private Button mBtnEnterCoordinates, mBtnUpdateSchedule;
    private TextView mTxtCoordinates, mTxtDay, mTxtDusk, mTxtNight;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mApp = App.getInstance();
        mPrefs = mApp.getPrefs();

        if (!App.getInstance().hasGpsPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        mTxtCoordinates = (TextView) findViewById(R.id.textCoordinates);
        updateCoordinatesUi(mPrefs.getFloat("lat", 181), mPrefs.getFloat("lon", 181));

        mTxtDay = (TextView) findViewById(R.id.textDay);
        mTxtDusk = (TextView) findViewById(R.id.textDusk);
        mTxtNight = (TextView) findViewById(R.id.textNight);

        updateSunriseSunsetScheduleUi(
                mPrefs.getString("sunrise", ""),
                mPrefs.getString("sunset", ""),
                mPrefs.getString("dusk_start", ""),
                mPrefs.getString("dusk_end", ""));

        mApp.updateSystemBrightness();

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(App.ACTION_LOCATION_UPDATE)) {
                    updateCoordinatesUi(
                            intent.getDoubleExtra("lat", 181), intent.getDoubleExtra("lon", 181));
                } else if (intent.getAction().equals(App.ACTION_SCHEDULE_UPDATE)) {
                    updateSunriseSunsetScheduleUi(
                            intent.getStringExtra("sunrise"),
                            intent.getStringExtra("sunset"),
                            intent.getStringExtra("dusk_start"),
                            intent.getStringExtra("dusk_end"));
                }
            }
        };

        mBtnEnterCoordinates = (Button) findViewById(R.id.enter_coordinates);
        mBtnUpdateSchedule = (Button) findViewById(R.id.update_schedule);

        mBtnEnterCoordinates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                LayoutInflater inflater = getLayoutInflater();
                final View dialogLayout = inflater.inflate(R.layout.dialog_enter_coordinates,
                        (ViewGroup) getCurrentFocus());

                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.dialog_title_enter_coordinates)
                        .setView(dialogLayout)
                        .setPositiveButton(R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText inputLat =
                                                (EditText) dialogLayout.findViewById(R.id.lat);
                                        EditText inputLon =
                                                (EditText) dialogLayout.findViewById(R.id.lon);

                                        checkCoordinates(inputLat.getText().toString(),
                                                inputLon.getText().toString());
                                    }
                                })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.cancel();
                                    }
                                })
                        .show();
            }
        });

        mBtnUpdateSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mApp.updateSystemBrightness();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mApp.setDebugEnabled(mPrefs.getBoolean("is_debug", false));
        registerReceiver(mBroadcastReceiver, new IntentFilter(App.ACTION_LOCATION_UPDATE));
        registerReceiver(mBroadcastReceiver, new IntentFilter(App.ACTION_SCHEDULE_UPDATE));
    }

    @Override
    public void onPause() {
        unregisterReceiver(mBroadcastReceiver);

        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPrefs = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_start_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateCoordinatesUi(double lat, double lon) {
        String text = getString(R.string.unknown);
        if (lat != 181 && lon != 181) {
            text = getString(R.string.coordinates_placeholder, lat, lon);
        }

        mTxtCoordinates.setText(text);
    }

    private void updateSunriseSunsetScheduleUi(String sunrise, String sunset,
                                               String duskStart, String duskEnd) {
        if (!sunrise.isEmpty() && !sunset.isEmpty() && !duskStart.isEmpty() && !duskEnd.isEmpty()
                && mTxtDay != null && mTxtDusk != null && mTxtNight != null) {
            mTxtDay.setText(
                    getString(R.string.time_placeholder, sunrise, sunset));
            mTxtDusk.setText(getString(R.string.times_placeholder,
                    duskStart, sunrise, sunset, duskEnd));
            mTxtNight.setText(
                    getString(R.string.time_placeholder, duskEnd, duskStart));

            mTxtDay.setTypeface(null, Typeface.NORMAL);
            mTxtDusk.setTypeface(null, Typeface.NORMAL);
            mTxtNight.setTypeface(null, Typeface.NORMAL);

            switch (mApp.getCurrentMode()) {
                case "day":
                    mTxtDay.setTypeface(null, Typeface.BOLD);
                    break;
                case "dusk":
                    mTxtDusk.setTypeface(null, Typeface.BOLD);
                    break;
                case "night":
                    mTxtNight.setTypeface(null, Typeface.BOLD);
                    break;
            }
        }
    }

    private void checkCoordinates(String strLat, String strLon) {
        try {
            float lat = Float.parseFloat(strLat);
            float lon = Float.parseFloat(strLon);

            if ((lat >= -180 && lat <= 180) && (lon >= -180 && lon <= 180)) {
                SharedPreferences.Editor prefsEditor = mPrefs.edit();
                prefsEditor.putFloat("lat", lat);
                prefsEditor.putFloat("lon", lon);
                mTxtCoordinates.setText(getString(R.string.coordinates_placeholder, lat, lon));
                prefsEditor.apply();

                mApp.updateSystemBrightness();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
