package kg.delletenebre.autobrightnesssunflower;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.dd.processbutton.iml.ActionProcessButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;

public class StartActivity extends AppCompatActivity {
    private final String TAG = getClass().getName();

    private final Context mContext = this;

    private SharedPreferences _settings;
    private APP mAPP;
    private final AQuery AQ = new AQuery(this);
    private ActionProcessButton btnUpdateLocation, btnEnterLocation, btnEnterCoordinates,
                                btnUpdateSchedule;
    private TextView txtLocation, txtCoordinates, txtDay, txtDusk, txtNight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        _settings = PreferenceManager.getDefaultSharedPreferences(this);

        mAPP = APP.getInstance(_settings);

        txtLocation = (TextView) findViewById(R.id.textLocation);
        String tLocation = _settings.getString("location", "");
        if (!tLocation.isEmpty()) {
            txtLocation.setText(tLocation);
        }
        txtCoordinates = (TextView) findViewById(R.id.textCoordinates);
        float lat = _settings.getFloat("lat", 181);
        float lon = _settings.getFloat("lon", 181);
        if (lat != 181 && lon != 181) {
            txtCoordinates.setText(getString(R.string.coordinates_placeholder, lat, lon));
        }

        mAPP.getCurrentDate();

        txtDay = (TextView) findViewById(R.id.textDay);
        txtDusk = (TextView) findViewById(R.id.textDusk);
        txtNight = (TextView) findViewById(R.id.textNight);
        String sunrise = _settings.getString("sunrise", "");
        String sunset = _settings.getString("sunset", "");
        String duskStart = _settings.getString("dusk_start", "");
        String duskEnd = _settings.getString("dusk_end", "");
        if (!sunrise.isEmpty() && !sunset.isEmpty() && !duskStart.isEmpty() && !duskEnd.isEmpty()) {
            String mode = mAPP.getCurrentMode();
            txtDay.setText(
                    getString(R.string.time_placeholder, sunrise, sunset));
            txtDusk.setText(getString(R.string.times_placeholder,
                    duskStart, sunrise, sunset, duskEnd));
            txtNight.setText(
                    getString(R.string.time_placeholder, duskEnd, duskStart));

            switch (mode) {
                case "day":
                    txtDay.setTypeface(null, Typeface.BOLD);
                    break;
                case "dusk":
                    txtDusk.setTypeface(null, Typeface.BOLD);
                    break;
                case "night":
                    txtNight.setTypeface(null, Typeface.BOLD);
                    break;
            }
        }

        mAPP.setSystemBrightness(mContext, getWindow());


        btnUpdateLocation = (ActionProcessButton) findViewById(R.id.update_location);
        btnUpdateLocation.setMode(ActionProcessButton.Mode.ENDLESS);

        btnEnterLocation = (ActionProcessButton) findViewById(R.id.enter_location);
        btnEnterLocation.setMode(ActionProcessButton.Mode.ENDLESS);

        btnEnterCoordinates = (ActionProcessButton) findViewById(R.id.enter_coordinates);

        btnUpdateSchedule = (ActionProcessButton) findViewById(R.id.update_schedule);
        btnUpdateSchedule.setMode(ActionProcessButton.Mode.ENDLESS);


        btnUpdateLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                btnUpdateLocation.setEnabled(false);
                btnUpdateLocation.setProgress(1);

                AQ.ajax("http://ip-api.com/json", JSONObject.class, new AjaxCallback<JSONObject>() {

                    @Override
                    public void callback(String url, JSONObject json, AjaxStatus status) {
                        btnUpdateLocation.setEnabled(true);
                        btnUpdateLocation.setProgress(0);


                        if (json != null) {
                            btnUpdateLocation.setProgress(100);
                            updateLocationInfo(json);

                        } else {
                            if (mAPP.isDEBUG()) {
                                Log.d(TAG, "Location error: " + String.valueOf(status.getCode()));
                            }

                            btnUpdateLocation.setProgress(-1);

                            if (mAPP.isTOAST()) {
                                Toast.makeText(AQ.getContext(),
                                        getString(R.string.toast_network_error),
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
            }
        });


        btnEnterLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnEnterLocation.setProgress(0);

                LayoutInflater inflater = getLayoutInflater();
                final View dialogLayout = inflater.inflate(R.layout.dialog_enter_location,
                        (ViewGroup) getCurrentFocus());


                new AlertDialog.Builder(mContext)
                        .setTitle(R.string.dialog_title_enter_location)
                        .setView(dialogLayout)
                        .setPositiveButton(getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        EditText input =
                                                (EditText) dialogLayout.findViewById(R.id.location);

                                        String location = input.getText().toString();
                                        if (location.isEmpty()) {
                                            if(mAPP.isDEBUG()) {
                                                Log.e(TAG, "Entered location error: Empty string");
                                            }
                                        } else {
                                            if(mAPP.isDEBUG()) {
                                                Log.d(TAG, "Entered location: " + location);
                                            }
                                            geolocateLocation(location);
                                        }

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

        btnEnterCoordinates.setOnClickListener(new View.OnClickListener() {
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

        btnUpdateSchedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mAPP.updateSunriseSunsetSchedule(mContext, btnUpdateSchedule,
                        txtDay, txtDusk, txtNight);
            }
        });

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

    private void updateLocationInfo(JSONObject json) {
        SharedPreferences.Editor _settingEditor = _settings.edit();
        String  resultLocation = getString(R.string.unknown),
                resultCoordinates = getString(R.string.unknown),
                sLocation = json.optString("display_name"),
                sCountry = json.optString("country"),
                sCity = json.optString("city"),
                sLat = json.optString("lat"),
                sLon = json.optString("lon");

        if (!sLat.isEmpty() && !sLon.isEmpty()) {
            float lat = Float.parseFloat(sLat);
            float lon = Float.parseFloat(sLon);
            resultCoordinates = getString(R.string.coordinates_placeholder,
                    lat, lon);
            _settingEditor.putFloat("lat", lat);
            _settingEditor.putFloat("lon", lon);

            if (!sLocation.isEmpty() || !sCountry.isEmpty() || !sCity.isEmpty()) {
                if (!sLocation.isEmpty()) {
                    resultLocation = sLocation;
                } else if (!sCountry.isEmpty() && !sCity.isEmpty()) {
                    resultLocation = sCountry + ", " + sCity;
                } else if (!sCountry.isEmpty()) {
                    resultLocation = sCountry;
                } else {
                    resultLocation = sCity;
                }

                _settingEditor.putString("location", resultLocation);
            } else {
                _settingEditor.remove("location");
            }
        } else {
            _settingEditor.remove("lat");
            _settingEditor.remove("lon");
            _settingEditor.remove("location");
        }

        txtLocation.setText(resultLocation);
        txtCoordinates.setText(resultCoordinates);

        _settingEditor.apply();

        if (mAPP.isDEBUG()) {
            Log.d(TAG, "Location display name: " + sLocation);
            Log.d(TAG, "Location latitude: " + sLat);
            Log.d(TAG, "Location longitude: " + sLon);
        }
    }

    private void geolocateLocation(String location) {
        try {
            String query = URLEncoder.encode(location, "utf-8");
            String url = "http://nominatim.openstreetmap.org/search/" + query + "?format=json";

            btnEnterLocation.setEnabled(false);
            btnEnterLocation.setProgress(1);

            AQ.ajax(url, JSONArray.class, new AjaxCallback<JSONArray>() {

                @Override
                public void callback(String url, JSONArray jsons, AjaxStatus status) {
                    if (mAPP.isDEBUG()) {
                        Log.d(TAG, "URL: " + url);
                    }

                    btnEnterLocation.setEnabled(true);

                    if (jsons != null && jsons.length() > 0) {
                        btnEnterLocation.setProgress(100);

                        try {
                            updateLocationInfo(jsons.getJSONObject(0));
                        } catch (Exception e) {
                            if (mAPP.isDEBUG()) {
                                Log.e(TAG, e.getMessage());
                            }

                            btnEnterLocation.setProgress(-1);

                            if (mAPP.isTOAST()) {
                                Toast.makeText(AQ.getContext(),
                                        R.string.toast_something_wrong,
                                        Toast.LENGTH_LONG).show();
                            }
                        }

                    } else {
                        if (mAPP.isDEBUG()) {
                            Log.e(TAG, "Location error: " + String.valueOf(status.getCode()));
                        }

                        btnEnterLocation.setProgress(-1);

                        if (mAPP.isTOAST()) {
                            Toast.makeText(AQ.getContext(),
                                    R.string.toast_network_error,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        } catch (Exception e) {
            if (mAPP.isDEBUG()) {
                Log.e(TAG, "Geocoder error: " + e.getMessage());
            }

            btnEnterLocation.setProgress(-1);
        }
    }

    private void checkCoordinates(String sLat, String sLon) {
        if (sLat.isEmpty() || sLon.isEmpty()) {
            if (mAPP.isDEBUG()) {
                Log.e(TAG, "Entered coordinate(s) is empty: "
                        + "[lat] " + sLat + ", " + "[lon] " + sLon);
            }
        } else {
            float lat = Float.parseFloat(sLat);
            float lon = Float.parseFloat(sLon);

            if ((lat >= -180 && lat <= 180) && (lon >= -180 && lon <= 180)) {
                if (mAPP.isDEBUG()) {
                    Log.d(TAG, "Entered coordinates: " + "[lat] " + sLat + ", " + "[lon] " + sLon);
                }

                SharedPreferences.Editor _settingEditor = _settings.edit();

                _settingEditor.putFloat("lat", lat);
                _settingEditor.putFloat("lon", lon);

                txtLocation.setText(R.string.unknown);
                txtCoordinates.setText(getString(R.string.coordinates_placeholder, lat, lon));

                _settingEditor.apply();

            } else {
                if (mAPP.isDEBUG()) {
                    Log.e(TAG, "Entered coordinate(s) is out of range: "
                            + "[lat] " + sLat + ", " + "[lon] " + sLon);
                }
            }
        }
    }

}
