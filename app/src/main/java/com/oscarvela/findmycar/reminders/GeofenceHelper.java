package com.oscarvela.findmycar.reminders;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.maps.model.LatLng;

public class GeofenceHelper {

    private static final String GEOFENCE_ID = "FIND_MY_CAR_GEOFENCE";
    private static final float GEOFENCE_RADIUS_IN_METERS = 50;

    /**
     * Crea y devuelve una solicitud de geovalla.
     * @param latLng Coordenadas para el centro de la geovalla.
     * @return Una instancia de GeofencingRequest.
     */
    public static GeofencingRequest getGeofencingRequest(LatLng latLng, PendingIntent pendingIntent) {
        Geofence geofence = new Geofence.Builder()
                // Establece un ID único para la geovalla.
                .setRequestId(GEOFENCE_ID)
                // Define el área circular de la geovalla.
                .setCircularRegion(
                        latLng.latitude,
                        latLng.longitude,
                        GEOFENCE_RADIUS_IN_METERS
                )
                // La geovalla persiste después de reiniciar el dispositivo.
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // Se activa solo cuando el usuario sale del área.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        return new GeofencingRequest.Builder()
                // Asegura que se active GEOFENCE_TRANSITION_EXIT si el dispositivo ya está fuera al crearla.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofence(geofence)
                .build();
    }

    /**
     * Crea un PendingIntent para el GeofenceBroadcastReceiver.
     * @param context Contexto de la aplicación.
     * @return Un PendingIntent.
     */
    public static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // Usamos FLAG_UPDATE_CURRENT para que el sistema actualice el PendingIntent
        // en lugar de crear uno nuevo cada vez.
        return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
    }
}
