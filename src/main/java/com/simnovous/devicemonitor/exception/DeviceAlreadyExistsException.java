package com.simnovous.devicemonitor.exception;

public class DeviceAlreadyExistsException extends RuntimeException {

    public DeviceAlreadyExistsException(String message) {
        super(message);
    }
}