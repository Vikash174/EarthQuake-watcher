package com.example.earthquakewatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;

import Util.Constants;
import model.EarthQuake;
import ui.CustomInfoWindow;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, LocationListener , GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;


    private static final int ALL_PERMISSION_RESULT = 0;
    private GoogleApiClient client;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private ArrayList<String> permissionToRequest;
    private final ArrayList<String>  permissions = new ArrayList<>();
    private final ArrayList<String> permissionRejected = new ArrayList<>();
    private AlertDialog.Builder dialogBuilder;
    private AlertDialog dialog;

    private LocationRequest locationRequest;


    private RequestQueue queue;

    public static final long UPDATE_INTERVAL = 5000;
    public static final long FASTEST_INTERVAL = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        queue = Volley.newRequestQueue(this);
        getEarthQuakes();


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // let's add permission we need to request location of users

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionToRequest = permissionToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (permissionToRequest.size()>0){
                requestPermissions(permissionToRequest.toArray(new String[0]),ALL_PERMISSION_RESULT);
            }
        }

        client = new GoogleApiClient.Builder(this).addApi(LocationServices.API)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();
    }

    private void getEarthQuakes() {

        EarthQuake earthQuake = new EarthQuake();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.URL,null,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            JSONArray  features = response.getJSONArray("features");
                            for (int i = 0; i<Constants.LIMIT; i++){
                                JSONObject properties = features.getJSONObject(i).getJSONObject("properties");

                              //Get geometry object

                                JSONObject geometry  = features.getJSONObject(i).getJSONObject("geometry");

                                // get coordinates  array
                                JSONArray coordinates = geometry.getJSONArray("coordinates");

                                double lon = coordinates.getDouble(0);
                                double lat = coordinates.getDouble(1);

//                                Log.d("Quakes",lon + " , " + lat);


                                earthQuake.setPlace(properties.getString("place"));
                                earthQuake.setType(properties.getString("type"));
                                earthQuake.setTime(properties.getLong("time"));
                                earthQuake.setMagnitude(properties.getDouble("mag"));
                                earthQuake.setDetailLink(properties.getString("detail"));

                                java.text.DateFormat dateFormat = java.text.DateFormat.getDateInstance();
                              String formattedDate =   dateFormat.format(new Date(properties.getLong("time")).getTime());

                              MarkerOptions markerOptions = new MarkerOptions();
                              markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
                              markerOptions.title(earthQuake.getPlace());
                              markerOptions.position(new LatLng(lat,lon));
                              markerOptions.snippet("Magnitude: "+ earthQuake.getMagnitude()+ "\n" + "Date: " + formattedDate );

                                Marker marker = mMap.addMarker(markerOptions);
                                marker.setTag(earthQuake.getDetailLink());
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,lon),1));


                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);


    }

    private ArrayList<String> permissionToRequest(ArrayList<String> wantedPermissions) {

        ArrayList<String> result  = new ArrayList<>();


        for (String perm : wantedPermissions){
            if (!hasPermission(perm)){
                result.add(perm);

            }
        }

        return  result;
    }
    private boolean hasPermission(String perm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            return checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
        }

        return  true;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        checkPlayServices();


    }


    @Override
    protected void onPause() {
        super.onPause();
        if (client != null  && client.isConnected()){
            LocationServices.getFusedLocationProviderClient(this)
                    .removeLocationUpdates(new LocationCallback());

            client.disconnect();
        }
    }



    private void checkPlayServices() {
        int errorCode = GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(this);

        if(errorCode != ConnectionResult.SUCCESS){
            Dialog errorDialog = GoogleApiAvailability.getInstance()
                    .getErrorDialog(this, errorCode, errorCode, new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {

                            Toast.makeText(MapsActivity.this, "NO services", Toast.LENGTH_SHORT).show();

                            finish();
                        }
                    });
        }
        else
            Toast.makeText(this, "All is well", Toast.LENGTH_SHORT).show();
    }




    @Override
    protected void onStart() {
        super.onStart();
        if (client != null){
            client.connect();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        client.disconnect();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new CustomInfoWindow(getApplicationContext()));
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onSuccess(Location location) {
                        // GEt last know location , But it could be null

                        if (location != null){

//                            locationTextView.setText("Lat" + location.getLatitude()+ "Lon:" + location.getLongitude());


                        }

                    }
                });
        starLocationUpdates();

    }

    private void starLocationUpdates() {
        locationRequest = new LocationRequest();

        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "You need to enable permission to display location", Toast.LENGTH_SHORT).show();
        }

        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest,new LocationCallback(){
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);


                        if(locationResult != null){
                            Location location  = locationResult.getLastLocation();
//                            locationTextView.setText(MessageFormat.format("Lat:{0} Lon: {1}", location.getLatitude(), location.getLongitude()));
                        }
                    }

                    @Override
                    public void onLocationAvailability(LocationAvailability locationAvailability) {
                        super.onLocationAvailability(locationAvailability);
                    }
                },null);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ALL_PERMISSION_RESULT) {
            for (String perm : permissionToRequest) {
                if (!hasPermission(perm)) {
                    permissionRejected.add(perm);
                }
            }

            if (permissionRejected.size() > 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(permissionRejected.get(0))) {
                        new AlertDialog.Builder(this)
                                .setMessage("These permissions are mandatory to get location")
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            requestPermissions(permissionRejected.toArray(new String[0]), ALL_PERMISSION_RESULT);
                                        }
                                    }
                                }).setNegativeButton("Cancel", null)
                                .create().show();

                    }
                }
            } else {
                if (client != null) {
                    client.connect();
                }
            }
        }
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        getQuakeDetails(Objects.requireNonNull(marker.getTag()).toString());

//        Toast.makeText(this, marker.getTitle().toString(), Toast.LENGTH_SHORT).show();

    }

    private void getQuakeDetails(String  url) {

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                String detailsUrl = "";

                try {
                    JSONObject properties = response.getJSONObject("properties");
                    JSONObject products = properties.getJSONObject("products");
                    JSONArray geoserve = products.getJSONArray("geoserve");

                    for (int i = 0; i < geoserve.length(); i++) {
                        JSONObject geoserveObj = geoserve.getJSONObject(i);

                        JSONObject contentObj = geoserveObj.getJSONObject("contents");
                        JSONObject geoJsonObj = contentObj.getJSONObject("geoserve.json");

                        detailsUrl = geoJsonObj.getString("url");


                    }
                    Log.d("URL: ", detailsUrl);

                    getMoreDetailed(detailsUrl);


                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        queue.add(jsonObjectRequest);
    }

    public void getMoreDetailed(String url){

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                url,null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                dialogBuilder = new AlertDialog.Builder(MapsActivity.this);
                View view = getLayoutInflater().inflate(R.layout.popup, null);

                Button dismissButton = (Button) view.findViewById(R.id.dismissPop);
                Button dismissButtonTop = (Button) view.findViewById(R.id.dismissPopup);
                TextView popList = (TextView) view.findViewById(R.id.popList);
                WebView htmlPop = (WebView) view.findViewById(R.id.htmlWebView);

                StringBuilder stringBuilder = new StringBuilder();


                try {

                    if (response.has("tectonicSummary")) {
                        response.getString("tectonicSummary");

                        JSONObject tectonic = response.getJSONObject("tectonicSummary");

                        if (tectonic.has("text")) {
                            tectonic.getString("text");

                            String text = tectonic.getString("text");

                            htmlPop.loadDataWithBaseURL(null, text, "text/html", "UTF-8", null);

                            Log.d("HTML", text);

                        }

                    }


                    JSONArray cities = response.getJSONArray("cities");

                    for (int i = 0; i < cities.length(); i++) {
                        JSONObject citiesObj = cities.getJSONObject(i);

                        stringBuilder.append("City: ").append(citiesObj.getString("name")).append("\n").append("Distance: ").append(citiesObj.getString("distance")).append("\n").append("Population: ").append(citiesObj.getString("population"));

                        stringBuilder.append("\n\n");

                    }

                    popList.setText(stringBuilder);

                    dismissButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dismissButtonTop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    dialogBuilder.setView(view);
                    dialog = dialogBuilder.create();
                    dialog.show();




                } catch (JSONException e) {
                    e.printStackTrace();
                }


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        queue.add(jsonObjectRequest);


    }


    @Override
    public boolean onMarkerClick(Marker marker) {

//        Toast.makeText(this, "marker is clicked", Toast.LENGTH_SHORT).show();
        return false;
    }
}