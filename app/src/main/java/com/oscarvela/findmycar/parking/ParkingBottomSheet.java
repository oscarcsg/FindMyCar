package com.oscarvela.findmycar.parking;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.oscarvela.findmycar.R;

public class ParkingBottomSheet extends BottomSheetDialogFragment {
    private ParkingListener listener;
    private boolean isParked = false;

    public void setListener(ParkingListener listener) {this.listener = listener;}

    public static ParkingBottomSheet newInstance(boolean isParked) {
        ParkingBottomSheet fragment = new ParkingBottomSheet();
        Bundle args = new Bundle();
        args.putBoolean("isParked", isParked);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            isParked = getArguments().getBoolean("isParked", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_bottom_sheet, container, false);

        TextInputEditText floorTxt = view.findViewById(R.id.floorTxt);
        TextInputEditText spotTxt = view.findViewById(R.id.spotTxt);
        Button confirmBtn = view.findViewById(R.id.confirmUbiBtn);
        Button deleteBtn = view.findViewById(R.id.deleteParkingBtn);

        // Configurar el OnClickListener para el botón de confirmar
        confirmBtn.setOnClickListener(v -> {
            if (listener != null) {
                listener.onParkingConfirmed(
                    floorTxt.getText() != null ? floorTxt.getText().toString() : "",
                    spotTxt.getText() != null ? spotTxt.getText().toString() : ""
                );
                dismiss();
                Toast.makeText(getContext(), R.string.toast_parking_saved, Toast.LENGTH_SHORT).show();
            }
        });

        // Mostrar el botón de borrar solo si ya hay un coche aparcado
        if (isParked) {
            deleteBtn.setVisibility(View.VISIBLE);
            deleteBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onParkingDeleted();
                }
                dismiss();
            });
        }

        return view;
    }
}
