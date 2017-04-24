package com.lcw.people;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.lcw.people.Helpers.PermissionRequestCode;

import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FloatingActionButton addFab;
    private ProgressBar progressBar;
    private MenuItem myLocationMenuItem;
    private MenuItem placeMenuItem;
    private LatLng location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        addFab = (FloatingActionButton) findViewById(R.id.addButton);
        addFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (location != null) {
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                            Uri.parse("google.navigation:q=" + location.latitude + "," + location.longitude));
                    v.getContext().startActivity(intent);
                }
            }
        });

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);

        myLocationMenuItem = menu.findItem(R.id.action_my_location);
        placeMenuItem = menu.findItem(R.id.action_place);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            myLocationMenuItem.setVisible(true);
        } else {
            myLocationMenuItem.setVisible(false);
        }

        if (location != null) {
            placeMenuItem.setVisible(true);
        } else {
            placeMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.action_place:
                if (location != null)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));
                return true;

            case R.id.action_my_location:
                enableMyLocation();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionRequestCode.ACCESS_FINE_LOCATION.getValue()) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (myLocationMenuItem != null) myLocationMenuItem.setVisible(false);
                mMap.setMyLocationEnabled(true);
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        String address = getIntent().getStringExtra("address");
        new SearchLocationAsyncTask().execute(address);
        enableMyLocation();
    }

    ////////////////////////////////////////
    //helper method
    ////////////////////////////////////////
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (myLocationMenuItem != null) myLocationMenuItem.setVisible(true);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PermissionRequestCode.ACCESS_FINE_LOCATION.getValue());
        } else {
            if (myLocationMenuItem != null) myLocationMenuItem.setVisible(false);
            mMap.setMyLocationEnabled(true);
        }
    }

    ////////////////////////////////////////
    //search async task
    ////////////////////////////////////////
    public class SearchLocationAsyncTask extends AsyncTask<String, Void, List<Address>> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Address> doInBackground(String... params) {
            Geocoder geocoder = new Geocoder(getBaseContext());
            try {
                return geocoder.getFromLocationName(params[0], 5);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            progressBar.setVisibility(View.GONE);

            if (addresses == null) {
                addFab.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), R.string.location_failed, Toast.LENGTH_LONG).show();
                return;
            }

            if (addresses.size() > 0) {
                addFab.setVisibility(View.VISIBLE);
                if (placeMenuItem != null) placeMenuItem.setVisible(true);

                // Add a marker in Sydney and move the camera
                location = new LatLng(addresses.get(0).getLatitude(), addresses.get(0).getLongitude());
                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(getResources().getString(R.string.here)));
                marker.showInfoWindow();
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15));


                Toast.makeText(getApplicationContext(), R.string.location_found, Toast.LENGTH_LONG).show();
            } else {
                addFab.setVisibility(View.GONE);
                Toast.makeText(getApplicationContext(), R.string.location_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }
}
