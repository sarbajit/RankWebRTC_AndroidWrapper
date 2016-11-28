package com.sarbajit.rankwebrtclib;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.MediaCodec;
import com.intel.webrtc.base.RemoteScreenStream;
import com.intel.webrtc.base.RemoteStream;
import com.intel.webrtc.base.WoogeenException;
import com.intel.webrtc.base.WoogeenIllegalArgumentException;
import com.intel.webrtc.conference.SubscribeOptions;
import com.intel.webrtc.conference.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by Rank on 17/11/16.
 */

class RoomHandler extends Handler {
    RoomHandlerCallback callback;
    OrientationEventListener orientationEventListener;


    public RoomHandler(Looper looper) {
        super(looper);
    }

    public RoomHandler(Looper looper, RoomHandlerCallback roomHandlerCallback) {
        super(looper);
        callback = roomHandlerCallback;
    }

    //web service for getting the token string
    String getToken(String basicServer, String roomId, String displayName){
        StringBuilder token = new StringBuilder("");
        URL url;
        HttpURLConnection httpURLConnection = null;
        HttpsURLConnection httpsURLConnection = null;
        try{
            if (WebRTCProperties.API_KEY.equalsIgnoreCase("")) {
                url = new URL(basicServer + "/createToken/");
            }
            else {
                url = new URL(basicServer +"/"+WebRTCProperties.API_KEY+ "/createToken/");
            }


            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setRequestProperty("Content-Type", "application/json");
            httpURLConnection.setRequestProperty("Accept", "application/json");
            httpURLConnection.setConnectTimeout(30000);
            httpURLConnection.setRequestMethod("POST");

            DataOutputStream out = new DataOutputStream(httpURLConnection.getOutputStream());
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("role", "presenter");
            jsonObject.put("username", displayName);
            jsonObject.put("room", roomId.equals("") ? "" : roomId);
            out.writeBytes(jsonObject.toString());
            out.flush();
            out.close();

            if(httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK){
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String lines;
                while((lines = reader.readLine()) != null){
                    lines = new String(lines.getBytes(), "utf-8");
                    token.append(lines);
                }
                reader.close();
            }

        }catch(MalformedURLException e){
            e.printStackTrace();
        }catch(ProtocolException e){
            e.printStackTrace();
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }catch(JSONException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        }finally{
            if(httpURLConnection != null){
                httpURLConnection.disconnect();
            }
        }

        return token.toString();
    }

    private void updateView() {
        int orientation = WebRTCProperties.conferenceActivity.getResources().getConfiguration().orientation;

        /*WebRTCProperties.conferenceActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                WebRTCProperties.remoteViewContainer.removeAllViews();
            }
        });*/

        int width = 0;
        int height = 0;

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            width = WebRTCProperties.scrWidth;
            height = WebRTCProperties.scrWidth;
        }
        else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            width = WebRTCProperties.scrHeight;
            height = WebRTCProperties.scrHeight;
        }
        else {
            width = WebRTCProperties.scrWidth;
            height = WebRTCProperties.scrWidth;
        }

        Log.e("updateView", width+"x"+height);

        createView(width, height, WebRTCProperties.subscribedStream);
    }

    private void createView(final int feedWidth, final int feedHeight, final RemoteStream remoteStream) {
        if (remoteStream == null)
            return;

        Log.e("createView", "remoteStreamId = " + remoteStream.getId() + ", from " + remoteStream.getRemoteUserId());

        if (remoteStream instanceof RemoteScreenStream) {
            Log.e("createView", ">>>>>>>>>>>>>>>>>>>>>> 5");
        }

        WebRTCProperties.conferenceActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(feedWidth, feedHeight);
                    params.addRule(RelativeLayout.CENTER_IN_PARENT);

                    Log.e("createview", WebRTCProperties.rootEglBase + " " + WebRTCProperties.rootEglBase.hasSurface());
                    WebRTCProperties.remoteStreamRenderer = new WoogeenSurfaceRenderer(WebRTCProperties.conferenceActivity);
                    WebRTCProperties.remoteStreamRenderer.setLayoutParams(params);
                    WebRTCProperties.remoteViewContainer.addView(WebRTCProperties.remoteStreamRenderer);
                    WebRTCProperties.remoteStreamRenderer.init(WebRTCProperties.rootEglBase.getEglBaseContext(), null);
                    remoteStream.attach(WebRTCProperties.remoteStreamRenderer);

                    Log.e("createview",WebRTCProperties.remoteViewContainer.getChildCount()+"");
                    for (int i=0; i<WebRTCProperties.remoteViewContainer.getChildCount(); i++) {
                        Log.e("Child", WebRTCProperties.remoteViewContainer.getChildAt(i).getClass()+"");
                    }

                } catch (WoogeenIllegalArgumentException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void handleMessage(Message msg) {

        switch (msg.what){
            case WebRTCProperties.MSG_LOGIN: {
                Bundle joinData = msg.getData();
                String roomId = joinData.getString("roomId");
                String displayName = joinData.getString("displayName");

                String portalUrl = WebRTCProperties.serverURL+":"+WebRTCProperties.serverPort;

                String tokenString = getToken(portalUrl, roomId, displayName);
                Log.e("msg_login", "token is : " + tokenString);

                WebRTCProperties.mRoom.join(tokenString, new ActionCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        callback.handlerCallback("joinSuccess",user);
                    }

                    @Override
                    public void onFailure(final WoogeenException e) {
                        callback.handlerCallback("joinFailure",e);
                    }
                });
                break;
            }
            case WebRTCProperties.MSG_SUBSCRIBE: {


                final SubscribeOptions option = new SubscribeOptions();

                option.setVideoCodec(MediaCodec.VideoCodec.VP8);

                RemoteStream remoteStream = (RemoteStream) msg.obj;

                if (remoteStream instanceof RemoteScreenStream) {
                    Log.e("subscribe", ">>>>>>>>>>>>>>>>>>>>>> 2");
                }

                Log.e("subscribe", "new stream :" + remoteStream.getRemoteUserId());

                if (WebRTCProperties.subscribedStreams == null) {
                    WebRTCProperties.subscribedStreams = new ArrayList<String>();
                }

                if (WebRTCProperties.subscribedStreams.contains(remoteStream.getId())) {
                    Log.e("subscribe", "stream already subscribed");
                    break;
                }

                if (remoteStream instanceof RemoteScreenStream) {
                    Log.e("subscribe", ">>>>>>>>>>>>>>>>>>>>>> 3");
                }

                WebRTCProperties.mRoom.subscribe(remoteStream, option,
                        new ActionCallback<RemoteStream>() {

                            @Override
                            public void onSuccess(final RemoteStream remoteStream) {

                                if (remoteStream instanceof RemoteScreenStream) {
                                    Log.e("subscribe", ">>>>>>>>>>>>>>>>>>>>>> 4");
                                }

                                WebRTCProperties.subscribedStreams.add(remoteStream.getId());


                                ViewTreeObserver observer = WebRTCProperties.remoteViewContainer.getViewTreeObserver();
                                observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

                                    @Override
                                    public void onGlobalLayout() {
                                        WebRTCProperties.remoteViewContainer.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                        WebRTCProperties.scrHeight = WebRTCProperties.remoteViewContainer.getMeasuredHeight();
                                        WebRTCProperties.scrWidth = WebRTCProperties.remoteViewContainer.getMeasuredWidth();

                                        Log.e("setVideoLayout", WebRTCProperties.scrWidth + "x" + WebRTCProperties.scrHeight);

                                        WebRTCProperties.subscribedStream = remoteStream;
                                        if (orientationEventListener == null) {
                                            orientationEventListener = new OrientationEventListener(WebRTCProperties.conferenceActivity) {
                                                @Override
                                                public void onOrientationChanged(int orientation) {
                                                    updateView();
                                                }
                                            };
                                        }
                                        updateView();
                                    }
                                });

                            }

                            @Override
                            public void onFailure(WoogeenException e) {
                                e.printStackTrace();
                                Log.e("subscribe fail", e.getMessage());
                            }

                        });
                if (remoteStream instanceof RemoteScreenStream) {
                    Log.e("subscribe", ">>>>>>>>>>>>>>>>>>>>>> 6");
                }

                break;
            }
            case WebRTCProperties.MSG_UNSUBSCRIBE:
            {
                final RemoteStream removedStream = (RemoteStream)msg.obj;

                WebRTCProperties.mRoom.unsubscribe(removedStream, new ActionCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        WebRTCProperties.subscribedStreams.remove(removedStream.getId());
                        updateView();
                    }

                    @Override
                    public void onFailure(WoogeenException e) {

                    }
                });
                break;
            }
            case WebRTCProperties.MSG_UNPUBLISH:
            {
                if (WebRTCProperties.localStream != null) {
                    WebRTCProperties.mRoom.unpublish(WebRTCProperties.localStream, new ActionCallback<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Message msg = new Message();
                            msg.what = WebRTCProperties.MSG_ROOM_DISCONNECTED;
                            WebRTCProperties.roomHandler.sendMessage(msg);
                        }

                        @Override
                        public void onFailure(WoogeenException e) {
                            Message msg = new Message();
                            msg.what = WebRTCProperties.MSG_ROOM_DISCONNECTED;
                            WebRTCProperties.roomHandler.sendMessage(msg);
                        }
                    });
                }
                break;
            }
            case WebRTCProperties.MSG_ROOM_DISCONNECTED:
            {

                WebRTCProperties.mRoom.leave(new ActionCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        WebRTCProperties.subscribedStreams.clear();
                        callback.handlerCallback("leaveSuccess", null);
                    }

                    @Override
                    public void onFailure(WoogeenException e) {
                        e.printStackTrace();
                        callback.handlerCallback("leaveFailure", e);
                    }

                });
                break;
            }
        }
    }
}
