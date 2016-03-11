package com.bendaschel.lab9;

import android.Manifest;
import android.app.FragmentManager;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, AwaitEvents.Callback, GoogleMap.OnCameraChangeListener{

    private static final String TAG = "MainActivity";
    private static final String EVENT_MAP_READY = "com.bendaschel.lab9.MAP_READY";
    private static final String EVENT_LOCATION_SERVICES_READY = "com.bendaschel.lab9.LOCATION_READY";
    private static final float ZOOM_LEVEL = 15.0f;
    private static final String KEY_LAST_CAMERA_STATE = "last_camera_state";
    private static final String KEY_MAP_TYPE = "last_map_type";
    private static final String KEY_MARKERS = "map_markers";
    private FragmentManager mFragmentManager;
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mGoogleMap;
    private AwaitEvents mAwaitEvents;
    private CameraPosition mLastCameraPosition;
    @Bind(R.id.layout_button_bar)
    LinearLayout mButtonBar;
    private Bundle mSavedInstanceState;

    ArrayList<LatLng> mMapMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mFragmentManager = getFragmentManager();
        mAwaitEvents = new AwaitEvents()
                .setListener(this)
                .awaitEvent(EVENT_MAP_READY)
                .awaitEvent(EVENT_LOCATION_SERVICES_READY);
        initMap(savedInstanceState);
        initLocationServices();
    }

    private void initMap(Bundle savedInstanceState) {
        MapFragment mapFragment = (MapFragment) mFragmentManager.findFragmentById(R.id.map);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_about:
                Toast.makeText(this, R.string.toast_about, Toast.LENGTH_LONG).show();
                return true;
        }
        return false;
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_LAST_CAMERA_STATE, mLastCameraPosition);
        outState.putInt(KEY_MAP_TYPE, mGoogleMap.getMapType());
        outState.putParcelableArrayList(KEY_MARKERS, mMapMarkers);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mLastCameraPosition = savedInstanceState.getParcelable(KEY_LAST_CAMERA_STATE);
        mSavedInstanceState = savedInstanceState;
    }

    private void restoreMapMarkers(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_MARKERS)) {
            ArrayList<Parcelable> markers= savedInstanceState.getParcelableArrayList(KEY_MARKERS);
            for (Parcelable marker: markers) {
                createMarkerAtLocation((LatLng) marker);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");
        mGoogleMap = googleMap;
        if (isLocationPermissionAllowed()) {
            googleMap.setMyLocationEnabled(true);
        }
        if (mSavedInstanceState != null) {
            googleMap.setMapType(mSavedInstanceState.getInt(KEY_MAP_TYPE));
        }
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setOnCameraChangeListener(this);
        restoreMapMarkers(mSavedInstanceState);
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

    private void restoreCameraPosition() {
        if (mLastCameraPosition != null) {
            CameraUpdate lastPosition = CameraUpdateFactory.newCameraPosition(mLastCameraPosition);
            mGoogleMap.moveCamera(lastPosition);
            return;
        }
        centerCameraOnLocation();
    }

    private boolean isLocationPermissionAllowed() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    private Location getLastKnownLocation() {
        if (! isLocationPermissionAllowed()){
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
    }
    /**
     * Code taken from Google example
     * https://developer.android.com/training/location/retrieve-current.html
     */
    private void centerCameraOnLocation() {
        Location lastLocation = getLastKnownLocation();
        if (lastLocation != null){
            LatLng newCameraPosition =
                    new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(newCameraPosition, ZOOM_LEVEL);
            mGoogleMap.animateCamera(update);
        }
    }

    /**
     * Fired when both Map is loaded and location services have been connected.
     */
    @Override
    public void onReady() {
        Log.d(TAG, "onReady: centering map on location");
        restoreCameraPosition();
        mButtonBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        mLastCameraPosition = cameraPosition;
    }

    @OnClick(R.id.btn_mark)
    public void markMap(View v) {
        if (!isLocationPermissionAllowed()) {
            return;
        }
        Location lastKnownLocation = getLastKnownLocation();
        if (lastKnownLocation != null) {
            LatLng markerPosition = new LatLng(lastKnownLocation.getLatitude(),
                    lastKnownLocation.getLongitude());
            createMarkerAtLocation(markerPosition);
        }
    }

    private void createMarkerAtLocation(LatLng markerPosition) {
        String markerTitle = Integer.toString(mMapMarkers.size());
        MarkerOptions marker = new MarkerOptions()
                .position(markerPosition)
                .title(markerTitle);
        mMapMarkers.add(markerPosition);
        mGoogleMap.addMarker(marker);
    }

    @OnClick(R.id.btn_change_type)
    public void changeMapType(View v) {
        int mapType = mGoogleMap.getMapType();
        int nextMapType;
        switch (mapType) {
            case GoogleMap.MAP_TYPE_NORMAL:
                nextMapType = GoogleMap.MAP_TYPE_SATELLITE;
                break;
            case GoogleMap.MAP_TYPE_SATELLITE:
                nextMapType = GoogleMap.MAP_TYPE_TERRAIN;
                break;
            case GoogleMap.MAP_TYPE_TERRAIN:
                nextMapType = GoogleMap.MAP_TYPE_NORMAL;
                break;
            default:
                nextMapType = GoogleMap.MAP_TYPE_NORMAL;
        }
        mGoogleMap.setMapType(nextMapType);
    }
}
