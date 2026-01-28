package com.oscarvela.findmycar.parking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.oscarvela.findmycar.R;

public class ParkingBottomSheet extends BottomSheetDialogFragment {
    private ParkingListener listener;
    public void setListener(ParkingListener listener) {this.listener = listener;}

    public ParkingBottomSheet() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout
        View view = inflater.inflate(
            R.layout.layout_bottom_sheet,
            container,
            false
        );

        // Buscar los elementos dentro de la vista
            // Lo hago de forma interna en el mét odo para ahorrar algo de memoria para no
            // tener objetos innecesarios siempre cargados
        TextInputEditText floorTxt = view.findViewById(R.id.floorTxt);
        TextInputEditText spotTxt = view.findViewById(R.id.spotTxt);
        Button confirmTxt = view.findViewById(R.id.confirmUbiBtn);

        confirmTxt.setOnClickListener(v -> {
            if (listener != null) {
                // Pasarle los datos a MainActivity, que es quien tiene los métodos para guardar los datos
                listener.onParkingConfirmed(
                    floorTxt.getText() != null ? floorTxt.getText().toString() : "",
                    spotTxt.getText() != null ? spotTxt.getText().toString() : ""
                );

                // Cerrar el panel
                dismiss();
            }
        });

        return view;
    }
}
