package com.htc.wallet.skrsdk.error;

public class DatabaseSaveException extends RuntimeException {

    public DatabaseSaveException() {
        super();
    }

    public DatabaseSaveException(String message) {
        super(message);
    }
}
