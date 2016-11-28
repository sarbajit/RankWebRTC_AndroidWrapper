package com.sarbajit.rankwebrtclib;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Message;
import android.widget.RelativeLayout;

import com.github.nkzawa.socketio.client.Socket;
import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.ClientContext;
import com.intel.webrtc.base.LocalCameraStream;
import com.intel.webrtc.base.LocalCameraStreamParameters;
import com.intel.webrtc.base.MediaCodec;
import com.intel.webrtc.base.WoogeenException;
import com.intel.webrtc.conference.ConferenceClient;
import com.intel.webrtc.conference.ConferenceClientConfiguration;
import com.intel.webrtc.conference.PublishOptions;
import com.intel.webrtc.conference.User;
import com.rank.socketlib.SocketLibrary;
import com.rank.socketlib.SocketListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.EglBase;
import org.webrtc.PeerConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Rank on 17/11/16.
 * WebRTC v3.2
 */
public class RankWebRTC implements RoomHandlerCallback {

    //public properties

    /**
     * URL of the WebRTC server, without '/' and port at the end. Must be set before 'joinConference'
     */
    public String serverURL = "";

    /**
     * Port of the WebRTC server. Must be set before 'joinConference'. Default value is 3004
     */
    public String serverPort = "3004";

    /**
     * URL of the SocketIO server, without '/' and port at the end. Must be set before 'createSocket'
     */
    public String socketURL = "";

    /**
     * Port of the SocketIO server. Must be set before 'createSocket'. Default value is 3000
     */
    public String socketPort = "3000";

    private JoinCallback joinCallback;
    private LeaveCallback leaveCallback;
    private ParticipantCallback participantCallback;
    StringBuilder result;

    //library and webrtc initialization

    /**
     * Inititalizes RankWebRTC library. Call at application start
     * @param context Application context. Actually the context of the activity, who is initilizing the library
     */
    public RankWebRTC(Context context) {
        //set eglbase object
        if(WebRTCProperties.rootEglBase == null)
        {
            WebRTCProperties.rootEglBase = EglBase.create();
        }

        //set app context
        ClientContext.setApplicationContext(context);
        //enable hw acc for h.264
        ClientContext.setCodecHardwareAccelerationEnabled(MediaCodec.VideoCodec.H264, false);
        //set eglbase context
        ClientContext.setVideoHardwareAccelerationOptions(WebRTCProperties.rootEglBase.getEglBaseContext());
        //initialize socket library
        WebRTCProperties.socketLib = new SocketLibrary();
        //webrtc config init
        init();
    }

    /**
     * Returns the API Key, which was set for WebRTC application
     */
    String getApiKey() {
        return WebRTCProperties.API_KEY;
    }

    /**
     * This sets the API Key for WebRTC Application
     * @param apiKey Unique API Key for a particular application
     */
    public void setApiKey(String apiKey) {
        WebRTCProperties.API_KEY = apiKey;
    }

    /**
     * Returns the server URL
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * This sets the WebRTC server URL Must be set before 'joinConference'
     * @param serverURL URL of the WebRTC server, without '/' and port at the end.
     */
    public void setServerURL(String serverURL) {
        if (serverURL.endsWith("/")){
            serverURL = serverURL.substring(0, serverURL.length()-1);
        }
        this.serverURL = serverURL;
        WebRTCProperties.serverURL = serverURL;
    }

    /**
     * Returns the server port
     */
    public String getServerPort() {
        return serverPort;
    }

    /**
     * This sets the port of the WebRTC server. Must be set before 'joinConference'.
     * @param serverPort Port of WebRTC server. Default value is 3004
     */
    public void setServerPort(String serverPort) {
        this.serverPort = serverPort;
        WebRTCProperties.serverPort = serverPort;
    }

    /**
     * Returns the SocketIO server URL
     */
    public String getSocketURL() {
        return socketURL;
    }

    /**
     * This sets the SocketIO server URL Must be set before 'createSocket'
     * @param socketURL URL of SocketIO server
     */
    public void setSocketURL(String socketURL) {
        if (socketURL.endsWith("/")){
            socketURL = socketURL.substring(0, socketURL.length()-1);
        }
        this.socketURL = socketURL;
    }

    /**
     * Returns the SocketIO server port
     */
    public String getSocketPort() {
        return socketPort;
    }

    /**
     * This sets the port of the SocketIO server. Must be set before 'createSocket'.
     * @param socketPort Port of SocketIO server. Default value is 3000
     */
    public void setSocketPort(String socketPort) {
        this.socketPort = socketPort;
    }

    //socket creation

    /**
     * Creates the socket in the specified SocketIO server, using socketName. Generally called after successful login to the app.
     * @param socketName Identifier by which socket will be created
     * @param socketListener Listener for the socket events.
     * @throws RankWebRTCException
     */
    public void createSocket(String socketName, SocketListener socketListener) throws RankWebRTCException {
        if (getSocketURL().equalsIgnoreCase(""))
            throw new RankWebRTCException("socketURL is empty", new Throwable("You must set socketURL property of RankWebRTC"));

        String urlString = socketURL+":"+socketPort;

        WebRTCProperties.socket = WebRTCProperties.socketLib.startSocket(urlString, socketName, socketListener);
    }

    //get socket if required

    /**
     * Returns the created socket
     */
    public Socket getSocket() {
        return WebRTCProperties.socket;
    }

    //socket disconnection

    /**
     * Removes the socket from the SocketIO server. Generally called after successful logout from the app.
     */
    public void removeStocket() {
        if (WebRTCProperties.socket != null)
            WebRTCProperties.socket.disconnect();
    }

    //set conference activity

    /**
     * Informs RankWebRTC library about the activity where conference will be held.
     * Call this method upon entering the conference activity.
     * @param activity
     */
    public void setConferenceActivity(Activity activity) {
        WebRTCProperties.conferenceActivity = activity;
        ClientContext.setApplicationContext(activity);
        AudioManager audioManager = ((AudioManager) activity.getSystemService(Context.AUDIO_SERVICE));
        audioManager.setSpeakerphoneOn(true);
    }

    /**
     * Informs RankWebRTC library about the activity where conference callbacks,
     * e.g. notifications of userJoin and userLeft will be received
     * @param conferenceListener
     */
    public void setConferenceListener(ConferenceListener conferenceListener) {
        WebRTCProperties.conferenceListener = conferenceListener;
    }

    void init() {
        WebRTCProperties.roomThread = new HandlerThread("Room Thread");
        WebRTCProperties.roomThread.start();
        WebRTCProperties.roomHandler = new RoomHandler(WebRTCProperties.roomThread.getLooper(), this);

        ConferenceClientConfiguration config = new ConferenceClientConfiguration();

        List<PeerConnection.IceServer> iceServers=new ArrayList<PeerConnection.IceServer>();
//        iceServers.add(new PeerConnection.IceServer(Common.stunAddr));

        try {
            config.setIceServers(iceServers);
        } catch (WoogeenException e1) {
            e1.printStackTrace();
        }

        WebRTCProperties.mRoom = new ConferenceClient(config);
        WebRTCProperties.conferenceCallback = new ConferenceCallback();
        WebRTCProperties.mRoom.addObserver(WebRTCProperties.conferenceCallback);
    }

    /**
     * This is used to join to a conference.
     * @param roomId Room ID, where the conference will be held
     * @param displayName The name to be displayed during conference
     * @param joinCallback Success/Failure callback for joining to a conference
     * @throws RankWebRTCException
     */
    public void joinConference(String roomId, String displayName, JoinCallback joinCallback) throws RankWebRTCException {
        Message msg = new Message();
        msg.what = WebRTCProperties.MSG_LOGIN;
        Bundle joinData = new Bundle();
        joinData.putString("roomId", roomId);
        joinData.putString("displayName", displayName);

        msg.setData(joinData);


//        if (getApiKey().equalsIgnoreCase(""))
//            throw new RankWebRTCException("API Key is empty", new Throwable("You must set apiKey property of RankWebRTC"));

        if (getServerURL().equalsIgnoreCase(""))
            throw new RankWebRTCException("serverURL is empty", new Throwable("You must set serverURL property of RankWebRTC"));

        this.joinCallback = joinCallback;
        WebRTCProperties.roomHandler.sendMessage(msg);
    }



    @Override
    public void handlerCallback(String status, Object callbackReturn) {
        if (status.equalsIgnoreCase("joinSuccess")) {
            joinCallback.joinSuccess((User) callbackReturn);
        }
        else if (status.equalsIgnoreCase("joinFailure")) {
            WoogeenException e = (WoogeenException) callbackReturn;
            joinCallback.joinFailure(new RankWebRTCException(e.getMessage()));
        }
        else if (status.equalsIgnoreCase("leaveSuccess")) {
            leaveCallback.leaveSuccess();
        }
        else if (status.equalsIgnoreCase("leaveFailure")) {
            WoogeenException e = (WoogeenException) callbackReturn;
            leaveCallback.leaveFailure(new RankWebRTCException(e.getMessage()));
        }
    }


    //for specifying the placeholders where videos will be rendered

    /**
     * This is to inform RankWebRTC library about the placeholders of the rendered video streams.
     * @param localContainer Placeholder for local camera feed
     * @param remoteContainer Placeholder for remote stream
     */
    public void setVideoLayout(RelativeLayout localContainer, RelativeLayout remoteContainer) {

        WebRTCProperties.localStreamRenderer = null;
        WebRTCProperties.localViewContainer = localContainer;
        WebRTCProperties.remoteViewContainer = remoteContainer;

        if (WebRTCProperties.localViewContainer != null) {
            if (WebRTCProperties.localStreamRenderer == null) {
                WebRTCProperties.localStreamRenderer = new WoogeenSurfaceRenderer(WebRTCProperties.conferenceActivity);
                WebRTCProperties.localViewContainer.addView(WebRTCProperties.localStreamRenderer);
                WebRTCProperties.localStreamRenderer.init(WebRTCProperties.rootEglBase.getEglBaseContext(), null);

            }
        }

    }


    /**
     * Get the participant list of a specified room
     * @param roomId
     * @param participantCallback Success/Failure callback for list fetching. Success callback returns the array of participant names
     */
    public void getParticipantList(final String roomId, final ParticipantCallback participantCallback){

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String basicServer = getServerURL()+":"+getServerPort()+"/";
                    URL url = new URL(basicServer + "getUsers/"+roomId);

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "application/json");

                    if (conn.getResponseCode() != 200) {
                        participantCallback.failure(new RankWebRTCException("Failed : HTTP error code : "
                                + conn.getResponseCode()));
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(
                            (conn.getInputStream())));

                    String output;
                    result = new StringBuilder();
//                    System.out.println("Output from Server .... \n");
                    while ((output = br.readLine()) != null) {
//                        System.out.println(output);
                        result.append(output);
                    }

                    conn.disconnect();

                    JSONArray jsonArray = new JSONArray(result.toString());
                    ArrayList<String> arrayList = new ArrayList<String>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        arrayList.add(jsonObject.getString("name"));
                    }

                    participantCallback.success(arrayList);

                } catch (MalformedURLException e) {

                    e.printStackTrace();

                } catch (IOException e) {

                    e.printStackTrace();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();


    }

    //publish video

    /**
     * This is used to publish own video
     * @param publishCallback Success/Failure callback for publishing video
     */
    public void publish(final PublishCallback publishCallback)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    LocalCameraStreamParameters msp = new LocalCameraStreamParameters(true,
                            true);
                    msp.setCamera(LocalCameraStreamParameters.CameraType.FRONT);
                    msp.setResolution(320, 240);
                    msp.setDisplayOrientation(WebRTCProperties.conferenceActivity);
                    WebRTCProperties.localStream = new LocalCameraStream(msp);

                    PublishOptions option = new PublishOptions();
                    option.setMaximumVideoBandwidth(300);
                    option.setMaximumAudioBandwidth(50);
                    option.setVideoCodec(MediaCodec.VideoCodec.VP8);

                    WebRTCProperties.mRoom.publish(WebRTCProperties.localStream, option, new ActionCallback<Void>() {

                        @Override
                        public void onSuccess(Void result) {
                            publishCallback.publishSuccess();
                        }

                        @Override
                        public void onFailure(WoogeenException e) {

                            if (WebRTCProperties.localStream != null) {
                                WebRTCProperties.localStream.close();
                                WebRTCProperties.localStreamRenderer.cleanFrame();
                                WebRTCProperties.localStream = null;
                                publishCallback.publishFailure(new RankWebRTCException(e.getMessage()));
                            }
                            e.printStackTrace();
                        }

                    });
                } catch (Exception e) {
                    if (WebRTCProperties.localStream != null) {
                        WebRTCProperties.localStream.close();
                        WebRTCProperties.localStreamRenderer.cleanFrame();
                        WebRTCProperties.localStream = null;
                    }
                    e.printStackTrace();

                }
            }
        }).start();

    }


    //leave conference

    /**
     * This is used to leave from a conference.
     * @param leaveCallback Success/Failure callback for leaving conference
     */
    public void leaveConference(LeaveCallback leaveCallback) {
        Message msg = new Message();
        msg.what = WebRTCProperties.MSG_UNPUBLISH;
        this.leaveCallback = leaveCallback;
        WebRTCProperties.roomHandler.sendMessage(msg);
    }


    //mute video
    /**
     * Used to pause outgoing camera stream
     */
    public void muteVideo() {
        WebRTCProperties.localStream.disableVideo();
    }


    //unmute video

    /**
     * Used to resume outgoing camera stream
     */
    public void unmuteVideo() {
        WebRTCProperties.localStream.enableVideo();
    }


    //mute audio

    /**
     * Used to mute microphone
     */
    public void muteAudio() {
        WebRTCProperties.localStream.disableAudio();
    }


    //unmute audio

    /**
     * Used to unmute microphone
     */
    public void unmuteAudio() {
        WebRTCProperties.localStream.enableAudio();
    }


    //mute speaker

    /**
     * Used to mute speaker
     */
    public void muteSpeaker() {
        WebRTCProperties.subscribedStream.disableAudio();
    }


    //unmute speaker

    /**
     * Used to unmute speaker
     */
    public void unmuteSpeaker() {
        WebRTCProperties.subscribedStream.enableAudio();
    }

    //interfaces

    //join conference callback
    /**
     * Callback methods while joining a conference
     */
    public interface JoinCallback {
        void joinSuccess(User user);
        void joinFailure(RankWebRTCException e);
    }


    //publish callback

    /**
     * Callback methods while publishing video
     */
    public interface PublishCallback {
        void publishSuccess();
        void publishFailure(RankWebRTCException e);
    }


    //leave callback

    /**
     * Callback methods while leaving a conference
     */
    public interface LeaveCallback {
        void leaveSuccess();
        void leaveFailure(RankWebRTCException e);
    }


    //participantlist callback

    public interface ParticipantCallback {
        void success(ArrayList<String> participantList);
        void failure(RankWebRTCException e);
    }
}
