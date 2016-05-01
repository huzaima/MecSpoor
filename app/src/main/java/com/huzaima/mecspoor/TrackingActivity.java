package com.huzaima.mecspoor;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.appyvet.rangebar.RangeBar;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class TrackingActivity extends FragmentActivity implements OnMapReadyCallback, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private GoogleMap googleMap;
    private String year, month, day, hour, minute;
    private String vehicleID;
    private Bundle b;
    private RangeBar rangeBar;
    private ArrayList<ArrayList<Double>> a = new ArrayList<>();
    private FloatingActionButton datePicker, timePicker;
    private SmoothProgressBar progressBar;
    private LinearLayout ll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        b = this.getIntent().getExtras();
        setContentView(R.layout.tracking_activity);

        year = getIntent().getStringExtra("Year");
        month = getIntent().getStringExtra("Month");
        day = getIntent().getStringExtra("Day");
        hour = getIntent().getStringExtra("Hour");
        minute = getIntent().getStringExtra("Minute");
        vehicleID = b.getString("VehicleId");
        if (hour.length() == 1) {
            hour = "0" + hour;
        }
        ll = (LinearLayout) findViewById(R.id.ll);
        progressBar = (SmoothProgressBar) findViewById(R.id.progressbar);
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        progressBar.setSmoothProgressDrawableStrokeWidth(px);
        px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0, getResources().getDisplayMetrics());
        progressBar.setSmoothProgressDrawableSeparatorLength(px);
        progressBar.setSmoothProgressDrawableSectionsCount(5);
        rangeBar = (RangeBar) findViewById(R.id.rangebar);
        rangeBar.setSeekPinByValue(Float.parseFloat(minute));
        rangeBar.setOnRangeBarChangeListener(new RangeBar.OnRangeBarChangeListener() {
            @Override
            public synchronized void onRangeChangeListener(RangeBar rangeBar, int i, int i2, String s, String s2) {
                trailDrawer(i, i2);
            }
        });
        rangeBar.setDrawTicks(false);
        findViewById(R.id.multiple_actions).bringToFront();
        LinearLayout ll = (LinearLayout) findViewById(R.id.ll);
        datePicker = (FloatingActionButton) findViewById(R.id.showDatePicker);
        datePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Calendar now = Calendar.getInstance();
                DatePickerDialog dpd = DatePickerDialog.newInstance(
                        TrackingActivity.this,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.setThemeDark(true);
                dpd.show(getFragmentManager(), "Test");
            }
        });
        timePicker = (FloatingActionButton) findViewById(R.id.showTimePicker);
        timePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                TimePickerDialog tpd = TimePickerDialog.newInstance(
                        TrackingActivity.this,
                        now.get(Calendar.HOUR_OF_DAY),
                        now.get(Calendar.MINUTE),
                        true
                );
                tpd.setThemeDark(true);
                tpd.show(getFragmentManager(), "Test2");
            }
        });
        new HistoryFetcher().execute();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap mapReady) {
        this.googleMap = mapReady;
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(b.<CameraPosition>getParcelable("Position")), 001, null);

    }

    @Override
    public void onDateSet(DatePickerDialog view, int year2, int monthOfYear, int dayOfMonth) {
        Calendar now = Calendar.getInstance();
        day = String.valueOf(dayOfMonth);
        month = String.valueOf(monthOfYear);
        year = String.valueOf(year2);

        TimePickerDialog tpd = TimePickerDialog.newInstance(
                TrackingActivity.this,
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );
        tpd.setThemeDark(true);
        tpd.show(getFragmentManager(), "Test2");
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute2, int second) {
        hour = String.valueOf(hourOfDay);
        minute = String.valueOf(minute2);
        rangeBar.setSeekPinByValue(Float.parseFloat(minute));
        new HistoryFetcher().execute();
    }

    public void trailDrawer(int m1, int m2) {
        googleMap.clear();
        ArrayList<LatLng> points = new ArrayList<>();
        LatLngBounds.Builder cameraSetter = new LatLngBounds.Builder();

        for (int i = 0; i < a.size(); i++) {

            if (a.get(i).get(2) >= m1 && a.get(i).get(2) <= m2) {
                points.add(new LatLng(a.get(i).get(0), a.get(i).get(1)));
                cameraSetter.include(new LatLng(a.get(i).get(0), a.get(i).get(1)));
            }
        }
        if (!points.isEmpty()) {
            googleMap.addMarker(new MarkerOptions().position(points.get(0)).icon((BitmapDescriptorFactory.fromResource(R.drawable.sgd))));
            googleMap.addMarker(new MarkerOptions().position(points.get(points.size() - 1)).icon((BitmapDescriptorFactory.fromResource(R.drawable.srd))));
            googleMap.addPolyline(new PolylineOptions().addAll(points).color(R.color.materialdesingcolor).width(20));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(cameraSetter.build(), 100));
        }
    }

    public class HistoryFetcher extends AsyncTask<Void, Void, Void> {

        String roadPoints = "";

        @Override
        protected void onPreExecute() {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8, 0);
            ll.setLayoutParams(lp);
            rangeBar.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.progressiveStart();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            String temp = "";
            a.clear();

            try {
                URL url = new URL("https://api.mongolab.com/api/1/databases/secondmecspoor/collections/vehicles_data?q={\"vehicle_id\":\"" + vehicleID + "\",\"timestamp.hour\":\"" + hour + "\",\"timestamp.date\":\"" + day + "\"}&apiKey=4R0C02NuwXrRDEIBgqLAUS5pHGkpBiKH");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                temp = rd.readLine();
                rd.close();
                con.disconnect();

            } catch (MalformedURLException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                JSONArray rootArray = new JSONArray(temp);
                for (int c = 0; c < rootArray.length(); c++) {
                    roadPoints += rootArray.getJSONObject(c).getJSONObject("location").getDouble("lat") + "," +
                            rootArray.getJSONObject(c).getJSONObject("location").getDouble("lng");
                    if (c < rootArray.length() - 1) {
                        roadPoints += "|";
                    }
                }

                URL url = new URL("https://roads.googleapis.com/v1/snapToRoads?path=" + roadPoints + "&interpolate=true&key=" + getString(R.string.google_maps_key));
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = rd.readLine()) != null)
                    response.append(line);
                rd.close();
                con.disconnect();

                ArrayList<Double> abc;
                JSONObject root = new JSONObject(response.toString());
                JSONArray array = root.getJSONArray("snappedPoints");
                double count = 0;
                for (int c = 0; c < array.length(); c++) {
                    abc = new ArrayList<>();
                    JSONObject bc = array.getJSONObject(c);
                    abc.add(bc.getJSONObject("location").getDouble("latitude"));
                    abc.add(bc.getJSONObject("location").getDouble("longitude"));
                    if (bc.has("originalIndex")) {
                        count = bc.getInt("originalIndex");
                        abc.add(count);
                    } else {
                        abc.add(count);
                    }
                    a.add(abc);
                }
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (ProtocolException e1) {
                e1.printStackTrace();
            } catch (JSONException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
            ll.setLayoutParams(lp);
            progressBar.progressiveStop();
            progressBar.setVisibility(View.GONE);
            rangeBar.setVisibility(View.VISIBLE);
            trailDrawer(0, Integer.parseInt(minute));

            if (a.size() <= 0) {
                Toast toast = Toast.makeText(getApplicationContext(), "Vehicle didn't move during specified time.", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
            }
        }
    }
}