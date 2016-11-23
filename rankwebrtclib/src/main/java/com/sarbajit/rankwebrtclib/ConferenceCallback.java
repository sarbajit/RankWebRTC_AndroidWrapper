package com.sarbajit.rankwebrtclib;

import android.os.Message;
import android.util.Log;

import com.intel.webrtc.base.RemoteCameraStream;
import com.intel.webrtc.base.RemoteScreenStream;
import com.intel.webrtc.base.RemoteStream;
import com.intel.webrtc.conference.ConferenceClient;
import com.intel.webrtc.conference.RemoteMixedStream;
import com.intel.webrtc.conference.User;

/**
 * Created by Rank on 17/11/16.
 */
public class ConferenceCallback implements ConferenceClient.ConferenceClientObserver {

    @Override
    public void onServerDisconnected() {
        if (WebRTCProperties.localStream != null)
        {
            WebRTCProperties.localStream.close();
            if (WebRTCProperties.localViewContainer != null) {
                WebRTCProperties.localStreamRenderer.cleanFrame();
            }

        }

        if (WebRTCProperties.subscribedStream != null)
        {
            WebRTCProperties.remoteStreamRenderer.cleanFrame();
        }

        WebRTCProperties.subscribedStreams.clear();

        Log.e("conference callback", "server disconnected");

    }

    @Override
    public void onStreamAdded(RemoteStream remoteStream) {
        Log.e("on stream added", remoteStream.getClass().getName());

        if (remoteStream instanceof RemoteCameraStream)
            return;
        Log.e("on stream added", "new stream added");
        Message msg = new Message();
        msg.what = WebRTCProperties.MSG_SUBSCRIBE;
        msg.obj = remoteStream;
        WebRTCProperties.roomHandler.sendMessage(msg);
    }

    @Override
    public void onStreamRemoved(RemoteStream remoteStream) {
        if (remoteStream instanceof RemoteScreenStream || remoteStream instanceof RemoteMixedStream) {
            return;
        }

        Message msg = new Message();
        msg.what = WebRTCProperties.MSG_UNSUBSCRIBE;
        msg.obj = remoteStream;
        WebRTCProperties.roomHandler.sendMessage(msg);
    }

    @Override
    public void onUserJoined(User user) {
        Log.e("conference callback", "new user :" + user.getId());

    }

    @Override
    public void onUserLeft(User user) {
        Log.e("conference callback", "left user :"+user.getId());
    }



    @Override
    public void onRecorderAdded(String s) {

    }

    @Override
    public void onRecorderRemoved(String s) {

    }

    @Override
    public void onRecorderContinued(String s) {

    }

    @Override
    public void onMessageReceived(String s, String s1, boolean b) {

    }
}
