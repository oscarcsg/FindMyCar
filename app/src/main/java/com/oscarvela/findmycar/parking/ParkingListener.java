package com.oscarvela.findmycar.parking;

public interface ParkingListener {
    void onParkingConfirmed(String floor, String spot);
    void onParkingDeleted();
}
