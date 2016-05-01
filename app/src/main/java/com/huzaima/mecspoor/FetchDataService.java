package com.huzaima.mecspoor;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

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
import java.util.HashMap;

import static com.google.android.gms.internal.zzir.runOnUiThread;

public class FetchDataService extends IntentService {

    boolean flag = true;
    public LatLngBounds bounds;

    public FetchDataService() {
        super("FetchDataService");
    }

    @Override
    public void onDestroy() {
        Log.i("MoreTesting", "before stop self ");
        flag = false;
        Log.i("MoreTesting", "after stop self ");
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.i("Dad", "Enterd doInBackground of Fetchdata");
        final MapsActivity parentActivity = (MapsActivity) MapsActivity.mapsActivity;
        String requestURL;
        String tempSuper = intent.getStringExtra("TempSuper");
        ArrayList<JSONObject> agencies = parentActivity.getAgencies();
        HashMap<String, Marker> vehicleMarker;

        bounds = parentActivity.getCurrentBounds();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                parentActivity.getGoogleMap().setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        bounds = parentActivity.getGoogleMap().getProjection().getVisibleRegion().latLngBounds;

                    }
                });

            }
        });

        vehicleMarker = parentActivity.getVehicleMarker();

        synchronized (this) {
            while (flag) {
                StringBuilder response = new StringBuilder();

                Log.i("Dad", "A new service");

                try {
                    if (bounds != null) {
                        requestURL = "https://transloc-api-1-2.p.mashape.com/vehicles.json?agencies=" + tempSuper + "&callback=call" +
                                "&geo_area=" + bounds.northeast.latitude + "%2C" + bounds.northeast.longitude + "%7C" +
                                +bounds.southwest.latitude + "%2C" + bounds.southwest.longitude;
                    } else {
                        requestURL = "https://transloc-api-1-2.p.mashape.com/vehicles.json?agencies=" + tempSuper + "&callback=call";
                    }

                    URL urll;
                    urll = new URL(requestURL);
                    HttpURLConnection connection = (HttpURLConnection) urll.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-Mashape-Key", "tUZfokqabXmshSiu88R3GDhx6Lydp1yIiwojsnhDtoN8mH6W7N");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.connect();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    response.append(rd.readLine());

                    connection.disconnect();
                    rd.close();
                } catch (MalformedURLException e) {
                } catch (ProtocolException e) {
                } catch (IOException e) {
                }
                int count = 0;
                try {
                    JSONObject rootObject = new JSONObject(response.toString());
                    JSONObject dataObject = rootObject.getJSONObject("data");
                    JSONArray vehiclesArray;
                    JSONObject currentVehicle;
                    for (int c = 0; c < agencies.size(); c++) {

                        if (dataObject.has(agencies.get(c).getString("agency_id"))) {

                            vehiclesArray = dataObject.getJSONArray(agencies.get(c).getString("agency_id"));

                            for (int d = 0; d < vehiclesArray.length(); d++) {
                                currentVehicle = vehiclesArray.getJSONObject(d);

                                if (currentVehicle.getString("tracking_status").contentEquals("up")) {

                                    final String agencyID = agencies.get(c).getString("agency_id");
                                    final String vehicleID = currentVehicle.getString("vehicle_id");
                                    currentVehicle.put("agency_id", agencyID);
                                    final double lat2 = Double.parseDouble(currentVehicle.getJSONObject("location").getString("lat"));
                                    final double lng2 = Double.parseDouble(currentVehicle.getJSONObject("location").getString("lng"));

                                    float heading;
                                    if (currentVehicle.isNull("heading")) {
                                        Log.i("value ofheading ", "was null omg");
                                        heading = 0;
                                    } else
                                        heading = Float.parseFloat(currentVehicle.getString("heading"));

                                    if (vehicleMarker.containsKey(vehicleID)) {
                                        if (((double) currentVehicle.get("speed") > 0))
                                            parentActivity.updateVehiclePosition(new LatLng(lat2, lng2), vehicleID, heading);
                                    } else {
                                        Thread.sleep(50);
                                        parentActivity.addVehicleToMap(new LatLng(lat2, lng2), agencyID, vehicleID);
                                    }//else
                                    count++;
                                }//if tracking status is up either update or add
                            }//for all vehicles
                        }//if agency exists
                    }//For all agencies
                } catch (JSONException e) {
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }//while
        }//Sync
        parentActivity.setVehicleMarker(vehicleMarker);
    }
}