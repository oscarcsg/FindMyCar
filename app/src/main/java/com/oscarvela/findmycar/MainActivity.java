package com.oscarvela.findmycar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.oscarvela.findmycar.parking.ParkingBottomSheet;
import com.oscarvela.findmycar.parking.ParkingListener;
import com.oscarvela.findmycar.reminders.GeofenceHelper;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class MainActivity extends AppCompatActivity implements ParkingListener {
    // ------------------------ //
    //         ATRIBUTOS        //
    // ------------------------ //
    private static final String TAG = "MainActivity";
    private MapView map = null;
    private MyLocationNewOverlay myLocationOverlay;
    private Marker parkingMarker = null;
    private GeofencingClient geofencingClient;
    private SharedPreferences prefs;
    private final int REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        prefs = getSharedPreferences("FindMyCarPrefs", MODE_PRIVATE);

        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("osmdroid", MODE_PRIVATE)
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        geofencingClient = LocationServices.getGeofencingClient(this);

        initMap();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ------------------------ //
    //           MAPA           //
    // ------------------------ //
    private void initMap() {
        map = findViewById(R.id.map);
        map.setMultiTouchControls(true);

        XYTileSource cartoDbVoyager = new XYTileSource(
                "CartoDB-Voyager", 0, 20, 256, ".png",
                new String[]{
                        "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                        "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
                }
        );
        map.setTileSource(cartoDbVoyager);

        map.getController().setZoom(18.0);

        if (hasFineLocationPermission()) {
            activateLocationOverlay();
            if (myLocationOverlay != null) map.getController().setCenter(myLocationOverlay.getMyLocation());
            else map.getController().setCenter(new GeoPoint(36.72016, -4.42034));
        } else {
            requestFineLocationPermission();
        }
        checkIfParkedAndRestore();
    }

    @SuppressLint("MissingPermission")
    private void activateLocationOverlay() {
        GpsMyLocationProvider provider = new GpsMyLocationProvider(this);
        provider.addLocationSource(LocationManager.NETWORK_PROVIDER);
        myLocationOverlay = new MyLocationNewOverlay(provider, map);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        myLocationOverlay.setDrawAccuracyEnabled(true);
        map.getOverlays().add(myLocationOverlay);
    }

    // ------------------------ //
    //         ACTIONS          //
    // ------------------------ //
    public void centerLocationAction(View view) {
        if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
            map.getController().animateTo(myLocationOverlay.getMyLocation());
            map.getController().setZoom(18.0);
        } else {
            Toast.makeText(this, getString(R.string.toast_waiting_for_gps_or_permission), Toast.LENGTH_SHORT).show();
        }
    }

    public void showConfigDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View configDialogView = getLayoutInflater().inflate(R.layout.dialog_configuration, null);
        builder.setView(configDialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        configDialogView.findViewById(R.id.confirmBtn).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void showParkingBottomSheet(View view) {
        if (myLocationOverlay == null || myLocationOverlay.getMyLocation() == null) {
            if (!hasFineLocationPermission()) {
                Toast.makeText(this, getString(R.string.toast_grant_location_permission_first), Toast.LENGTH_LONG).show();
                checkAndRequestPermissions();
            } else {
                Toast.makeText(this, getString(R.string.toast_wait_for_gps), Toast.LENGTH_SHORT).show();
            }
            return;
        }

        boolean isParked = prefs.getBoolean("IS_PARKED", false);
        ParkingBottomSheet bottomSheet = ParkingBottomSheet.newInstance(isParked);
        bottomSheet.setListener(this);
        bottomSheet.show(getSupportFragmentManager(), "ParkingSheet");
    }

    // ------------------------ //
    //         METHODS          //
    // ------------------------ //
    private void drawParkingMarker(GeoPoint location, String floor, String spot) {
        if (parkingMarker != null) map.getOverlays().remove(parkingMarker);
        parkingMarker = new Marker(map);
        parkingMarker.setPosition(location);
        parkingMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        parkingMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_car_marker));
        parkingMarker.setTitle(formatMarkerMsg(floor, spot));
        map.getOverlays().add(parkingMarker);
        map.invalidate();
    }

    private String formatMarkerMsg(String floor, String spot) {
        boolean hasFloor = !floor.isEmpty();
        boolean hasSpot = !spot.isEmpty();
        if (hasFloor && hasSpot) return getString(R.string.marker_full, floor, spot);
        if (hasFloor) return getString(R.string.marker_floor_only, floor);
        if (hasSpot) return getString(R.string.marker_spot_only, spot);
        return getString(R.string.marker_default);
    }

    private void saveParkingData(double lat, double lon, String floor, String spot) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putFloat("LAT", (float) lat);
        editor.putFloat("LON", (float) lon);
        editor.putString("FLOOR", floor);
        editor.putString("SPOT", spot);
        editor.putBoolean("IS_PARKED", true);
        editor.apply();
    }

    private void checkIfParkedAndRestore() {
        if (prefs.getBoolean("IS_PARKED", false)) {
            double lat = prefs.getFloat("LAT", 0);
            double lon = prefs.getFloat("LON", 0);
            if (lat != 0 && lon != 0) {
                drawParkingMarker(new GeoPoint(lat, lon), prefs.getString("FLOOR", ""), prefs.getString("SPOT", ""));
            }
        }
    }

    // ------------------------ //
    //     GEOFENCING METHODS   //
    // ------------------------ //
    @SuppressLint("MissingPermission")
    private void addGeofence(LatLng latLng) {
        if (!hasFineLocationPermission() || !hasBackgroundLocationPermission()) {
            Toast.makeText(this, getString(R.string.toast_missing_permissions_for_reminder), Toast.LENGTH_LONG).show();
            return;
        }
        PendingIntent pendingIntent = GeofenceHelper.getPendingIntent(this);
        geofencingClient.addGeofences(GeofenceHelper.getGeofencingRequest(latLng, pendingIntent), pendingIntent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geovalla añadida con éxito.");
                    Toast.makeText(MainActivity.this, getString(R.string.toast_reminder_activated), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al añadir la geovalla: " + e.getMessage());
                    String errorMessage = getString(R.string.toast_reminder_activation_failed, e.getMessage());
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    private void removeGeofence() {
        PendingIntent pendingIntent = GeofenceHelper.getPendingIntent(this);
        geofencingClient.removeGeofences(pendingIntent)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geovalla eliminada con éxito.");
                    Toast.makeText(MainActivity.this, getString(R.string.toast_reminder_deactivated), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al eliminar la geovalla: " + e.getMessage());
                    String errorMessage = getString(R.string.toast_reminder_deactivation_failed, e.getMessage());
                    Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                });
    }

    // ------------------------ //
    //      CICLO DE VIDA       //
    // ------------------------ //
    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    // ------------------------ //
    //         PERMISOS         //
    // ------------------------ //
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, getString(R.string.toast_location_permission_granted), Toast.LENGTH_SHORT).show();
                activateLocationOverlay();
                checkAndRequestBackgroundPermission();
            } else {
                Toast.makeText(this, getString(R.string.toast_location_permission_required), Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean hasFineLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestFineLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS}, REQUEST_FINE_LOCATION_PERMISSIONS_REQUEST_CODE);
    }

    private void checkAndRequestPermissions() {
        if (!hasFineLocationPermission()) {
            requestFineLocationPermission();
        } else {
            checkAndRequestBackgroundPermission();
        }
    }

    private void checkAndRequestBackgroundPermission() {
        if (hasBackgroundLocationPermission()) {
            Toast.makeText(this, getString(R.string.toast_all_permissions_granted), Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_background_permission_title)
                .setMessage(R.string.dialog_background_permission_message)
                .setPositiveButton(R.string.dialog_button_go_to_settings, (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.dialog_button_not_now, (dialog, which) -> Toast.makeText(MainActivity.this, getString(R.string.toast_background_permission_denied), Toast.LENGTH_LONG).show())
                .create()
                .show();
        }
    }

    // ------------------------ //
    //     IMPLEMENTACIONES     //
    // ------------------------ //
    @Override
    public void onParkingConfirmed(String floor, String spot) {
        if (!hasFineLocationPermission() || !hasBackgroundLocationPermission()) {
            checkAndRequestPermissions();
            Toast.makeText(this, getString(R.string.toast_location_permission_needed_retry), Toast.LENGTH_LONG).show();
            return;
        }

        GeoPoint currentLocation = myLocationOverlay.getMyLocation();
        if (currentLocation == null) {
            Toast.makeText(this, getString(R.string.toast_could_not_get_location), Toast.LENGTH_SHORT).show();
            return;
        }

        saveParkingData(currentLocation.getLatitude(), currentLocation.getLongitude(), floor, spot);
        drawParkingMarker(currentLocation, floor, spot);
        addGeofence(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        Toast.makeText(this, getString(R.string.toast_parking_saved), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onParkingDeleted() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("LAT");
        editor.remove("LON");
        editor.remove("FLOOR");
        editor.remove("SPOT");
        editor.putBoolean("IS_PARKED", false);
        editor.apply();

        if (parkingMarker != null) {
            map.getOverlays().remove(parkingMarker);
            parkingMarker = null;
            map.invalidate();
        }
        removeGeofence();
        Toast.makeText(this, getString(R.string.toast_parking_deleted), Toast.LENGTH_SHORT).show();
    }
}