package com.htc.wallet.skrsdk.drives;

public interface DriveCallback<Result> {
    void onComplete(final Result result);

    void onFailure(Exception e);
}
