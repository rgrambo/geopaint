package edu.uw.rgrambo.geopaint;

import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

public class MapsActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, OnMapReadyCallback {

    private GoogleMap mMap;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;

    private boolean mRequestingLocationUpdates;
    private boolean mPenDrawing;

    private Stack<Polyline> mAllPolylines;
    private PolylineOptions mCurrentPolylineOptions;

    private ColorPicker cp;

    private int mCurrentColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mPenDrawing = false;

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        mLocationRequest = LocationRequest.create()
            .setInterval(10000)
            .setFastestInterval(5000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mAllPolylines = new Stack<>();

        cp = new ColorPicker(MapsActivity.this, 0, 0, 0);

        // Set the default color to white
        mCurrentColor = R.color.colorPrimary;

        Log.e("Tag", "Step 1");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.colorButton:
                colorPicker();
                return true;
            case R.id.saveButton:
                saveLines();
                return true;
            case R.id.drawButton:
                if (mPenDrawing) {
                    findViewById(R.id.drawButton).setBackgroundColor(getResources().getColor(
                            R.color.disabledButton));
                    penUp();
                } else {
                    findViewById(R.id.drawButton).setBackgroundColor(getResources().getColor(
                            R.color.enabledButton));
                    penDown();
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateUI() {
        if (mAllPolylines.peek() != null)
        {
            mAllPolylines.pop().remove();
        }

        mAllPolylines.push(mMap.addPolyline(mCurrentPolylineOptions));
    }

    protected void startLocationUpdates() {
        Log.e("Tag", "Step 2");
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {

            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            Log.e("Tag", "Step 2.5");
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    // If the pen is drawing, stop it and save the line
    public void penUp() {
        if (mPenDrawing) {
            mPenDrawing = false;
        }
    }

    // If the pen is not drawing, start it drawing
    public void penDown() {
        if (!mPenDrawing) {
            mPenDrawing = true;
            mCurrentPolylineOptions = new PolylineOptions()
                .width(5)
                .color(mCurrentColor);
        }
    }

    public void penColorChange(String hex) {
        penUp();
        mCurrentColor = Color.parseColor(hex);
        penDown();
    }

    public void colorPicker() {
        cp.show();

        Button okColor = (Button)cp.findViewById(R.id.okColorButton);
        okColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCurrentColor = cp.getColor();

                findViewById(R.id.colorButton).setBackgroundColor(mCurrentColor);

                cp.dismiss();
            }
        });
    }

    // Design decision made to stop the current drawing
    public void saveLines() {
        penUp();

        String result = "[";

        Set<Polyline> lines = (Set<Polyline>) mAllPolylines.clone();

        for (Polyline line : lines) {
            result += "{ \"type\": \"LineString\", \"coordinates\": [";

            boolean first = true;

            for (LatLng latLng : line.getPoints()) {
                if (!first) {
                    result += ", ";
                } else {
                    first = false;
                }

                result += "[" + latLng.latitude + ", " + latLng.longitude + "]";
            }

            result += "}";
        }

        result += "]";

        try {
            File file = new File(this.getExternalFilesDir(null), "drawing.geojson");
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(result.getBytes()); //write the string to the file
            outputStream.close(); //close the stream
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

        if (mCurrentLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude())));
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("Tag", "Step 3");
        mCurrentLocation = location;
        if (mPenDrawing) {
            mCurrentPolylineOptions.add(new LatLng(
                    mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude()));
            updateUI();
        }
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        mRequestingLocationUpdates = true;
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        mRequestingLocationUpdates = false;
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0 ){
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
