package com.sarbajit.rankwebrtclib;

/**
 * Created by Rank on 17/11/16.
 */

import android.app.Activity;
import android.os.HandlerThread;
import android.widget.RelativeLayout;

import com.github.nkzawa.socketio.client.Socket;
import com.intel.webrtc.base.LocalCameraStream;
import com.intel.webrtc.base.RemoteStream;
import com.intel.webrtc.conference.ConferenceClient;
import com.rank.socketlib.SocketLibrary;

import org.webrtc.EglBase;

import java.util.ArrayList;

class WebRTCProperties {

    static EglBase rootEglBase;
    static Socket socket;
    static SocketLibrary socketLib;
    static Activity conferenceActivity;
    static String serverURL = "";
    static String serverPort = "3004";
    static String API_KEY = "";


    static final int MSG_ROOM_DISCONNECTED = 98;
    static final int MSG_PUBLISH = 99;
    static final int MSG_LOGIN = 100;
    static final int MSG_SUBSCRIBE = 101;
    static final int MSG_UNSUBSCRIBE = 102;
    static final int MSG_UNPUBLISH = 103;


    static RoomHandler roomHandler;
    static ConferenceClient mRoom;
    static ConferenceCallback conferenceCallback;
    static ConferenceListener conferenceListener;
    static HandlerThread roomThread;


    static RelativeLayout localViewContainer;
    static RelativeLayout remoteViewContainer;
    static WoogeenSurfaceRenderer localStreamRenderer;
    static LocalCameraStream localStream;
    static WoogeenSurfaceRenderer remoteStreamRenderer;
    static RemoteStream subscribedStream;
    static ArrayList<String> subscribedStreams;

    public static int scrWidth;
    public static int scrHeight;
}
