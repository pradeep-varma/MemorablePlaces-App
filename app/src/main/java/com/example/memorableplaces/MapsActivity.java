package com.example.memorableplaces;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.memorableplaces.databinding.ActivityMapsBinding;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastknownlocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                centerMapOnLocation(lastknownlocation,"Your location");
            }
        }
    }

    public void centerMapOnLocation(Location location, String title){
        LatLng userlocation=new LatLng(location.getLatitude(),location.getLongitude());
        mMap.clear();
        if(title!="Your location") {
            mMap.addMarker(new MarkerOptions().position(userlocation).title(title));
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userlocation,10));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        Intent intent=getIntent();
        if(intent.getIntExtra("placenumber",0)==0){
            locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
            locationListener=new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    centerMapOnLocation(location,"Your location");
                }
            };
            if(Build.VERSION.SDK_INT<23){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
            }else{
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);
                    Location lastknownlocation=locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    centerMapOnLocation(lastknownlocation,"Your location");
                }else {
                    ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
                }
            }
        }else{
            Location placelocation=new Location(LocationManager.GPS_PROVIDER);
            placelocation.setLatitude(MainActivity.locations.get(intent.getIntExtra("placenumber",0)).latitude);
            placelocation.setLongitude(MainActivity.locations.get(intent.getIntExtra("placenumber",0)).longitude);
            centerMapOnLocation(placelocation,MainActivity.place.get(intent.getIntExtra("placenumber",0)));
        }
    }
    @Override
    public void onMapClick(LatLng latLng) {
        Geocoder geocoder=new Geocoder(getApplicationContext(),Locale.getDefault());
        String address="";
        try {
            List<Address> addressList=geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
            if (addressList != null && addressList.size() > 0) {
            if(addressList.get(0).getThoroughfare()!=null) {
                if (addressList.get(0).getThoroughfare()!= null) {
                    address += addressList.get(0).getSubThoroughfare() + " ";
                }
                address += addressList.get(0).getThoroughfare();
            }
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (address == "") {
            SimpleDateFormat dateFormat=new SimpleDateFormat("HH:mm yyyy-MM-dd");
            address=dateFormat.format(new Date());
        }
        mMap.addMarker(new MarkerOptions().position(latLng).title(address).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        MainActivity.place.add(address);
        MainActivity.locations.add(latLng);
        SharedPreferences sharedPreferences=this.getSharedPreferences(" com.example.memorableplaces",Context.MODE_PRIVATE);
        try {
            ArrayList<String> latitudes=new ArrayList<>();
            ArrayList<String> longitudes=new ArrayList<>();
            for(LatLng coordinates:MainActivity.locations){
                latitudes.add(Double.toString(coordinates.latitude));
                longitudes.add(Double.toString(coordinates.longitude));
            }
            sharedPreferences.edit().putString("place",ObjectSerializer.serialize(MainActivity.place)).apply();
            sharedPreferences.edit().putString("latitudes",ObjectSerializer.serialize(latitudes)).apply();
            sharedPreferences.edit().putString("longitudes",ObjectSerializer.serialize(longitudes)).apply();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MainActivity.arrayAdapter.notifyDataSetChanged();
        Toast.makeText(this,"Location saved",Toast.LENGTH_SHORT).show();
    }

}