package com.example.runusandroid;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.runusandroid.ActivityRecognition.UserActivityBroadcastReceiver;
import com.example.runusandroid.ActivityRecognition.UserActivityTransitionManager;
import com.example.runusandroid.databinding.ActivityMain2Binding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity2 extends AppCompatActivity {

    private ActivityMain2Binding binding;
    private FusedLocationProviderClient fusedLocationClient;
    UserActivityTransitionManager activityManager;
    PendingIntent pendingIntent;
    UserActivityBroadcastReceiver activityReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("user_id", null);

        if(userId == null) {
            // 로그인되어 있지 않으면 LoginActivity로 전환
            Intent intent = new Intent(MainActivity2.this, LoginActivity.class);
            startActivity(intent);
            finish();  // MainActivity2를 종료
            return;
        }

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main2);
        NavigationUI.setupWithNavController(binding.navView, navController);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1000);
            }
        }
        // Request location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        activityManager = new UserActivityTransitionManager(this);
        pendingIntent = PendingIntent.getBroadcast(
                this,
                UserActivityTransitionManager.CUSTOM_REQUEST_CODE_USER_ACTION,
                new Intent(UserActivityTransitionManager.CUSTOM_INTENT_USER_ACTION),
                PendingIntent.FLAG_MUTABLE
        );
        activityManager.registerActivityTransitions(pendingIntent);
        activityReceiver = new UserActivityBroadcastReceiver();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(UserActivityTransitionManager.CUSTOM_INTENT_USER_ACTION);
        this.registerReceiver(activityReceiver, filter, RECEIVER_EXPORTED);
    }

    @Override
    protected void onPause() {
        activityManager.removeActivityTransitions(pendingIntent);
        super.onPause();
    }
    @Override
    protected void onStop(){
        unregisterReceiver(activityReceiver);
        super.onStop();
    }

    public FusedLocationProviderClient getFusedLocationClient() {
        return fusedLocationClient;
    }

    // Get the last location. Currently printing log only. TODO: return the location
    public void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            // Handle location update here
            if (location != null) {
                Log.d("test:location:main", "Location:" + location.getLatitude() + ", " + location.getLongitude());
            } else {
                Log.d("test:location:main", "Location failed");
            }
        });
    }

}