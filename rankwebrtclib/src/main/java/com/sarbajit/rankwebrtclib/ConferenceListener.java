package com.sarbajit.rankwebrtclib;

import com.intel.webrtc.conference.User;

/**
 * Created by Rank on 28/11/16.
 */
public interface ConferenceListener {
    void onUserJoined(User user);
    void onUserLeft(User user);
}
