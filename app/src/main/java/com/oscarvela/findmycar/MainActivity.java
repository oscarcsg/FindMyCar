package com.oscarvela.findmycar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MapView map = null;
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // Cargar OSM antes de inflar el layout
        Configuration.getInstance().load(
            getApplicationContext(),
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        );
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_main);

        // Inicializar el mapa
        map = findViewById(R.id.map);
        XYTileSource cartoDbVoyager = new XYTileSource(
            "CartoDB-Voyager",// Nombre
            0, 20,     // Zoom mínimo y máximo
            256,                     // Tamaño de los cuadros (pixels)
            ".png",                  // Formato de imagen
            new String[]{
                    "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                    "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                    "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
            });
        map.setTileSource(cartoDbVoyager);

        // Centrar el mapa en un punto por defecto (Málaga por ahora), luego pondré para que coja la ubi del usuario
        map.getController().setZoom(18.0);
        map.getController().setCenter(new GeoPoint(36.72016, -4.42034));

        // pedir permisos
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE // Necesario para android antiguo
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // ------------------------ //
    //      CICLO DE VIDA       //
    // ------------------------ //
    // Estos métodos son necesarios para OSM
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
    private void requestPermissionsIfNecessary(String[] permissions) {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permisos concedidos. Cargando ubicación...", Toast.LENGTH_SHORT).show();
                // Aquí irá la activación del "Mi ubicación"
            } else {
                Toast.makeText(this, "Necesitas dar permisos para usar la app.", Toast.LENGTH_LONG).show();
            }
        }
    }
}