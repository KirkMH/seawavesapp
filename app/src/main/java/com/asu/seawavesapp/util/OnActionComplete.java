package com.asu.seawavesapp.util;

public interface OnActionComplete {
    void onComplete(boolean success);
    void onError(Throwable t);
}