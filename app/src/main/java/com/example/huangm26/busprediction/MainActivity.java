package com.example.huangm26.busprediction;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;

import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransitMode;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Step;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;


import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {
    public static GoogleMap mMap;
    private EditText srcStop;
    private EditText destStop;
    private LinearLayout predictButton;
    private TextView resultText;
    private CircularProgressView progressView;
    String TAG = "MainActivity";
    public static List<Polyline> routeList;
    public static String srcID;
    public static String destID;
    public static String srcStopName;
    public static String destStopName;
    String addressAppend = ", New York";
    String serverUrl = "http://d98c6e52.ngrok.io/";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        srcStop = (EditText) findViewById(R.id.startLocation);
        destStop = (EditText) findViewById(R.id.endLocation);
        predictButton = (LinearLayout) findViewById(R.id.predictButton);
        resultText = (TextView) findViewById(R.id.resultText);
        progressView = (CircularProgressView) findViewById(R.id.progressView);
        predictButton.setOnClickListener(this);
        routeList = new ArrayList<Polyline>();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    public void requestPermissions(@NonNull String[] permissions, int requestCode)
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                return;
            }
        }
        mMap.setMyLocationEnabled(true);

        LatLng nyc = new LatLng(40.7, -73.9);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(nyc));
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.predictButton:
                clearDisplay();
                progressView.setVisibility(View.VISIBLE);
                srcID = srcStop.getText().toString();
                destID = destStop.getText().toString();
                if (srcID == null || destID == null) {
                    Toast.makeText(this, "Please enter a stop code", Toast.LENGTH_SHORT).show();
                    return;
                }
                queryTime();
                queryStartStop();
//                displayRoute("Astor Pl/3 Av, New York, NY","WEST END AV/WEST 70 ST, New York, NY");
                break;
            default:
                break;
        }
    }

    private void clearDisplay() {
        resultText.setText("");
        resultText.setVisibility(View.GONE);
        for (Polyline line : routeList) {
            line.remove();
        }
        routeList.clear();
        srcID = "";
        destID = "";
        srcStopName = "";
        destStopName = "";
    }


    private void queryStartStop() {
        String base_url = "https://bustime.mta.info/m/index?q=";
        Ion.with(getApplicationContext())
                .load(base_url + srcID)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        int startIndex = result.indexOf(srcID) + 7;
                        int endIndex = result.indexOf("</title>");
                        Log.d(TAG, result.substring(startIndex, endIndex));
                        if(startIndex >= endIndex) {
                            Toast.makeText(getApplicationContext(),"Please enter a valid stop code", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        srcStopName = result.substring(startIndex, endIndex) + addressAppend;
                        queryFinishStop();
                    }
                });
    }

    private void queryFinishStop() {
        String base_url = "https://bustime.mta.info/m/index?q=";
        Ion.with(this)
                .load(base_url + destID)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        int startIndex = result.indexOf(destID) + 7;
                        int endIndex = result.indexOf("</title>");
                        Log.d(TAG, result.substring(startIndex, endIndex));
                        if(startIndex >= endIndex) {
                            Toast.makeText(getApplicationContext(),"Please enter a valid stop code", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        destStopName = result.substring(startIndex, endIndex) + addressAppend;
                        new DrawRoute(getApplicationContext()).execute(srcStopName, destStopName);
                    }
                });
    }

    private void queryTime() {
        Log.d(TAG,"Queryting time");
        Ion.with(getApplicationContext())
                .load(serverUrl + "gettime")
                .setBodyParameter("srcID", srcID)
                .setBodyParameter("destID", destID)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        // do stuff with the result or error
                        Log.d(TAG,"Result is " + result);
                        displayTime(result);
                    }
                });
    }

    private void displayTime(String result) {
        progressView.setVisibility(View.GONE);
        resultText.setVisibility(View.VISIBLE);
        resultText.setText(result + " mins");
    }

    private class DrawRoute extends AsyncTask<String, Void, Void> {

        private Context context;

        public DrawRoute(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(String... params) {
            if (params.length < 2) {
                return null;
            }
            String srcStop = params[0];
            String destStop = params[1];
            displayRoute(srcStop, destStop);
            return null;
        }

        private void displayRoute(String startLoc, String endLoc) {
            Log.d(TAG, "Getting address");
            Geocoder coder = new Geocoder(context);
            List<Address> address;
            LatLng origin = null;
            LatLng destination = null;
            try {
                List<Address> geoResults = coder.getFromLocationName(startLoc, 1);
                while (geoResults.size() == 0) {
                    geoResults = coder.getFromLocationName(startLoc, 1);
                }
                if (geoResults.size() > 0) {
                    Address addr = geoResults.get(0);
                    Log.d(TAG, "Got start address");
                    origin = new LatLng(addr.getLatitude(), addr.getLongitude());
                }
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
            try {
                List<Address> geoResults = coder.getFromLocationName(endLoc, 1);
                while (geoResults.size() == 0) {
                    geoResults = coder.getFromLocationName(endLoc, 1);
                }
                if (geoResults.size() > 0) {
                    Address addr = geoResults.get(0);
                    Log.d(TAG, "Got stop address");
                    destination = new LatLng(addr.getLatitude(), addr.getLongitude());
                }
            } catch (Exception e) {
                System.out.print(e.getMessage());
            }
            String serverKey = "AIzaSyDFOQkYCfgv6X5PHML0TervjOa3InoiBC0";
            if (origin == null || destination == null) {
                Log.d(TAG, "Failed to load address");
            }
            GoogleDirection.withServerKey(serverKey)
                    .from(origin)
                    .to(destination)
                    .transportMode(TransportMode.TRANSIT)
                    .transitMode(TransitMode.BUS)
                    .execute(new DirectionCallback() {
                        @Override
                        public void onDirectionSuccess(Direction direction, String rawBody) {
                            Log.d(TAG, "success");
                            if (direction.isOK()) {
                                List<Step> stepList = direction.getRouteList().get(0).getLegList().get(0).getStepList();
                                Log.d(TAG, stepList.toString());
                                ArrayList<PolylineOptions> polylineOptionList = DirectionConverter.createTransitPolyline(context, stepList, 5, Color.RED, 3, Color.BLUE);
                                for (PolylineOptions polylineOption : polylineOptionList) {
                                    Polyline line = mMap.addPolyline(polylineOption);
                                    routeList.add(line);
                                }
                            }
                        }

                        @Override
                        public void onDirectionFailure(Throwable t) {
                            // Do something here
                        }
                    });
        }

    }


}
