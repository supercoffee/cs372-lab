package com.bendaschel.lab9;

import android.Manifest;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;


public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, AwaitEvents.Callback {

    private static final String TAG = "MainActivity";
    private static final String EVENT_MAP_READY = "com.bendaschel.lab9.MAP_READY";
    private static final String EVENT_LOCATION_SERVICES_READY = "com.bendaschel.lab9.LOCATION_READY";
    private static final float ZOOM_LEVEL = 15.0f;
    private FragmentManager mFragmentManager;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private AwaitEvents mAwaitEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFragmentManager = getFragmentManager();
        mAwaitEvents = new AwaitEvents()
                .setListener(this)
                .awaitEvent(EVENT_MAP_READY)
                .awaitEvent(EVENT_LOCATION_SERVICES_READY);
        initMap();
        initLocationServices();
    }

    private void initMap() {
        MapFragment mapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment_container, mapFragment);
        fragmentTransaction.commit();
        mapFragment.getMapAsync(this);
    }

    private void initLocationServices() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        mGoogleMap = googleMap;
        mAwaitEvents.fireEvent(EVENT_MAP_READY);
    }


    @Override
    public void onConnected(Bundle bundle) {
        mAwaitEvents.fireEvent(EVENT_LOCATION_SERVICES_READY);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed" + connectionResult.getErrorMessage());
    }

    /**
     * Code taken from Google example
     * https://developer.android.com/training/location/retrieve-current.html
     */
    private void centerCameraOnLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng newCameraPosition =
                new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(newCameraPosition, ZOOM_LEVEL);
        mGoogleMap.animateCamera(update);
    }

    /**
     * Fired when both Map is loaded and location services have been connected.
     */
    @Override
    public void onReady() {
        Log.d(TAG, "onReady: centering map on location");
        centerCameraOnLocation();
    }
}
