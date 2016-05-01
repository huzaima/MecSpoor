package com.huzaima.mecspoor;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    static String tempSuper;
    public static ArrayList<JSONObject> agencies = null;
    private boolean stop = false;
    public static GoogleMap googleMap;
    private List<Marker> agencyMarker = new ArrayList<>();
    public static HashMap<String, Marker> vehicleMarker = new HashMap<>();
    LatLngBounds currentBounds = null;
    boolean callIt = true;

    Calendar now = Calendar.getInstance();
    String year, month, day, hour, minute;
    Bundle b = new Bundle();
    public static Activity mapsActivity;
    Intent serviceIntent;
    ImageView mImageView;
    ViewSwitcher switcher;

    @Override
    protected void onDestroy() {
        //stopService(serviceIntent);
        super.onDestroy();
        Log.v("lifecycle", "onDestory");
    }

    @Override
    protected void onPause() {
        callIt = false;
        super.onPause();
        stop = true;
        if (serviceIntent != null)
            stopService(serviceIntent);
        Log.v("lifecycle", "onPause");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        switcher = (ViewSwitcher) findViewById(R.id.viewSwitcher);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        mapsActivity = this;
        callIt = true;

//        switcher.showNext();

        mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switcher.showNext();
                startService(serviceIntent);
            }
        });


    }//OnCreate

    @Override
    protected void onStart() {
        super.onStart();
        Log.v("lifecycle", "onStart");
        stop = false;

    }

    public HashMap<String, Marker> getVehicleMarker() {
        return vehicleMarker;
    }

    public void setVehicleMarker(HashMap<String, Marker> hm) {
        vehicleMarker = hm;
    }

    public ArrayList<JSONObject> getAgencies() {
        return agencies;
    }

    public LatLngBounds getCurrentBounds() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
            }
        });
        return currentBounds;
    }

    @Override
    public void onMapReady(final GoogleMap mapReady) {

        this.googleMap = mapReady;
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.99374, -83.01612), 15));
        currentBounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        try {
            new FetchAndDrawAgencies().execute().get();
        } catch (InterruptedException e) {

        } catch (ExecutionException e) {

        }
        //mHandler.post(runnable);


        serviceIntent = new Intent(getApplicationContext(), FetchDataService.class);
        //  myBundle.putParcelable("GoogleMap", (android.os.Parcelable) vehicleMarker);
        //myBundle.putParcelable("GoogleMap",  this);

        serviceIntent.putExtra("TempSuper", tempSuper);

        startService(serviceIntent);

        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {

                b.putParcelable("Position", googleMap.getCameraPosition());
                b.putString("VehicleId", marker.getTitle());


                DatePickerDialog dpd = DatePickerDialog.newInstance(
                        MapsActivity.this,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.setThemeDark(true);
                dpd.show(getFragmentManager(), "Test");

            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
            }

        });//On Marker Drag

    }//onMapReady


    @Override
    public void onDateSet(DatePickerDialog datePickerDialog, int i, int i2, int i3) {

        TimePickerDialog tpd = TimePickerDialog.newInstance(
                MapsActivity.this,
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
        );
        tpd.setThemeDark(true);
        tpd.show(getFragmentManager(), "Test2");
        year = String.valueOf(i);
        month = String.valueOf(i2);
        day = String.valueOf(i3);

    }

    public GoogleMap getGoogleMap() {
        return googleMap;
    }


    @Override
    public void onTimeSet(RadialPickerLayout radialPickerLayout, int i, int i2, int i3) {

        hour = String.valueOf(i);
        minute = String.valueOf(i2);


        Intent intent = new Intent(MapsActivity.this, TrackingActivity.class);
        intent.putExtras(b);
        intent.putExtra("Year", year);
        intent.putExtra("Month", month);
        intent.putExtra("Day", day);
        intent.putExtra("Hour", hour);
        intent.putExtra("Minute", minute);

        startActivity(intent);

    }


    @Override
    protected void onResume() {
        super.onResume();
        stop = false;
        if (vehicleMarker != null)
            vehicleMarker.clear();
        if (!callIt)
            startService(serviceIntent);
        Log.v("lifecycle", "onResume");
    }


    public void addAgencyToMap(final LatLng latLng, final String agencyID, final String longName, final String shortName, final String url) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              agencyMarker.add(googleMap.addMarker(new MarkerOptions().position(latLng)
                                      .title(longName)
                                      .snippet(agencyID + " " + shortName + " " + url)
                                      .visible(true)
                                      .icon(BitmapDescriptorFactory.fromResource(R.drawable.mini_vista))));
                          }
                      }
        );
    }


    public class FetchAndDrawAgencies extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            String requestURL;
            StringBuilder response = new StringBuilder();
            double lat, lng;
            String agencyID, shortName, longName, url;

            try {
                requestURL = "https://transloc-api-1-2.p.mashape.com/agencies.json?callback=call";
                URL urll;
                urll = new URL(requestURL);
                HttpURLConnection connection = (HttpURLConnection) urll.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-Mashape-Key", "tUZfokqabXmshSiu88R3GDhx6Lydp1yIiwojsnhDtoN8mH6W7N");
                connection.setRequestProperty("Accept", "application/json");
                connection.connect();
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                response.append(rd.readLine());
                agencies = parseAgencies(response.toString());
                connection.disconnect();
                rd.close();
            } catch (MalformedURLException e) {
            } catch (ProtocolException e) {
            } catch (IOException e) {
            }

            if (agencies != null)
                for (int c = 0; c < agencies.size(); c++) {
                    try {
                        lat = agencies.get(c).getJSONObject("position").getDouble("lat");
                        lng = agencies.get(c).getJSONObject("position").getDouble("lng");
                        agencyID = agencies.get(c).getString("agency_id");
                        shortName = agencies.get(c).getString("short_name");
                        longName = agencies.get(c).getString("long_name");
                        url = agencies.get(c).getString("url");
                        addAgencyToMap(new LatLng(lat, lng), agencyID, longName, shortName, url);
                    } catch (JSONException e1) {
                    }
                }//For loop through all agencies

            return null;
        }//Do in Background

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.i("Marib", "Going into post exevute of fetchAndDrawAgencies data");
        }
    }//fetchAndDrawAgencies


    public void addVehicleToMap(final LatLng latLng, final String agencyID, final String vehicleID) {

        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {

                              vehicleMarker.put(vehicleID, googleMap.addMarker(new MarkerOptions().position(latLng)
                                      .title(vehicleID)
                                      .snippet("Vehicle#" + vehicleID + " belongs to agency#" + agencyID)
                                      .visible(true)
                                      .draggable(true)
                                      .flat(true)
                                      .rotation(0)
                                      .icon(BitmapDescriptorFactory.fromResource(R.drawable.ok_car))));
                              Log.i("ClassCA", "Add vehicle " + vehicleID);
                          }

                      }
        );

    }

    public void updateVehiclePosition(final LatLng latlng, final String vehicleID, final float heading) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                vehicleMarker.get(vehicleID).setPosition(latlng);

                Log.i("VlaueOfHeading", String.valueOf(heading));
                vehicleMarker.get(vehicleID).setRotation(heading);

                Log.i("ClassCA", "Updating vehicle " + vehicleID);
            }
        });
    }


    public static ArrayList<JSONObject> parseAgencies(final String jsonString) {
        if (jsonString == null)
            return null;
        final ArrayList<JSONObject> agencies = new ArrayList<>();

        StringBuilder temp = new StringBuilder();
        try {
            JSONObject rootObject = new JSONObject(jsonString);
            JSONArray dataArray = rootObject.getJSONArray("data");
            for (int c = 0; c < dataArray.length(); c++) {
                if (c < dataArray.length() - 1)
                    temp.append(dataArray.getJSONObject(c).getString("agency_id")).append("%2C");
                agencies.add(dataArray.getJSONObject(c));
            }
            temp.append(dataArray.getJSONObject(dataArray.length() - 1).getString("agency_id"));
            tempSuper = temp.toString();
        } catch (JSONException e) {
        }
        return agencies;
    }

}