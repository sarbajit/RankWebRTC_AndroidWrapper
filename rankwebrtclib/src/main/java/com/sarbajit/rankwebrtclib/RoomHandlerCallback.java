package com.sarbajit.rankwebrtclib;

/**
 * Created by Rank on 18/11/16.
 */
public interface RoomHandlerCallback<T> {
    void handlerCallback(String status, T callbackReturn);
}
