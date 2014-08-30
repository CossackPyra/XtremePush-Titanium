package com.krilab.xtremepush;

import java.util.ArrayList;
import java.util.HashMap;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.FragmentManager;
import com.squareup.otto.Subscribe;
import org.appcelerator.kroll.*;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.*;
import org.appcelerator.titanium.proxy.ActivityProxy;
import org.appcelerator.titanium.proxy.IntentProxy;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupportHelper;
import org.appcelerator.titanium.util.TiConvert;
import ie.imobile.extremepush.PushConnector;
import ie.imobile.extremepush.api.model.EventsPushlistWrapper;
import ie.imobile.extremepush.api.model.PushMessage;
import ie.imobile.extremepush.api.model.PushmessageListItem;
import ie.imobile.extremepush.ui.XPushLogActivity;
import ie.imobile.extremepush.util.LocationAccessHelper;


@Kroll.module(name = "XtremePushTitanium", id = "com.krilab.xtremepush")
public class XtremePushTitaniumModule extends KrollModule {
    private static final String LCAT = "XtremePushTitaniumModule";

    private PushConnector pushConnector = null;
    private boolean registered = false;


    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {
        ie.imobile.extremepush.util.XR.init(app);
    }

    @Kroll.method
    public void registerForRemoteNotifications(@Kroll.argument(optional=true) Object args) {
        TiApplication app = TiApplication.getInstance();
        TiProperties properties = app.getAppProperties();

        String appKey = properties.getString("XtremePushApplicationKey", null);
        String projectNumber = properties.getString("GoogleProjectNumber", null);
        if (appKey == null || projectNumber == null) {
            throw new IllegalArgumentException("XtremePushApplicationKey or GoogleProjectNumber missed in tiapp.xml");
        }

        TiRootActivity activity = app.getRootActivity();
        FragmentManager fragmentManager = activity.getSupportFragmentManager();

        Object locationTimeoutValue = null;
        Object locationDistanceValue = null;
        if (args instanceof HashMap) {
            HashMap<String, Object> options = (HashMap<String, Object>) args;
            locationTimeoutValue = options.get("locationTimeoutValue");
            locationDistanceValue = options.get("locationDistanceValue");
        }


        class NewIntentCallback implements KrollEventCallback {
            @Override
            public void call(Object o) {
                Log.i(LCAT, "new intent callback");

                KrollDict data = (KrollDict) o;
                IntentProxy ip = (IntentProxy) data.get(TiC.PROPERTY_INTENT);
                Intent intent = ip.getIntent();
                pushConnector.onNewIntent(intent);
            }
        }

        class StartLocationHandler implements TiActivityResultHandler {
            @Override
            public void onResult(Activity activity, int requestCode, int resultCode, Intent data) {
                Log.i(LCAT, "start location callback");

                pushConnector.onActivityResult(requestCode, resultCode, data);
            }

            @Override
            public void onError(Activity activity, int requestCode, Exception e) {
                Log.e(LCAT, "start location error");
            }
        }

        ActivityProxy activityProxy = activity.getActivityProxy();
        activityProxy.addEventListener(TiC.EVENT_NEW_INTENT, new NewIntentCallback());

        // getSupportHelper() is protected so try to search it to register START_LOCATION_ACTIVITY_CODE
        activity.getUniqueResultCode(); // init support helper
        for (int i = 0; i< 100; i++) { // search activity helper
            TiActivitySupportHelper activitySupportHelper = TiActivitySupportHelpers.retrieveSupportHelper(activity, i);
            if (activitySupportHelper == null) continue;

            StartLocationHandler startLocationHandler = new StartLocationHandler();
            int locationActivityCode = LocationAccessHelper.START_LOCATION_ACTIVITY_CODE;
            activitySupportHelper.registerResultHandler(locationActivityCode, startLocationHandler);
        }


        if (locationTimeoutValue != null && locationDistanceValue != null) {
            int locationTimeout = TiConvert.toInt(locationTimeoutValue);
            int locationDistance = TiConvert.toInt(locationDistanceValue);
            pushConnector = PushConnector.init(fragmentManager, appKey, projectNumber, locationTimeout, locationDistance);
        } else {
            pushConnector = PushConnector.init(fragmentManager, appKey, projectNumber);
        }

        initNotificationMessageReceivers();
        // @WARN: not valid now because no callbacks to PushConnector.init() :(
        registered = true;
    }

    @Kroll.method
    public void unregisterForRemoteNotifications() {
        pushConnector = null;
        registered = false;
    }

    @Kroll.getProperty
    public boolean getIsSandboxModeOn() {
        Log.w(LCAT, "isSandboxModeOn not implemented in Android");
        return false;
    }

    @Kroll.getProperty
    public Object getVersion() {
        Log.w(LCAT, "version not implemented in Android");
        return null;
    }

    @Kroll.getProperty
    public boolean getShouldWipeBadgeNumber() {
        Log.w(LCAT, "shouldWipeBadgeNumber not implemented in Android");
        return false;
    }

    @Kroll.setProperty
    public void setShouldWipeBadgeNumber(Object arg) {
        Log.w(LCAT, "shouldWipeBadgeNumber not implemented in Android");
    }

    @Kroll.getProperty
    public HashMap getDeviceInfo() {
        if (!registered) {
            Log.w(LCAT, "Please call registerForRemoteNotifications() first");
            return null;
        }

        // @WARN: can crash if no deviceToken or XPushDeviceID
        // and we can't check it now
        return pushConnector.getDeviceInfo();
    }

    @Kroll.setProperty
    public void setLocationEnabled(Object arg) {
        Log.w(LCAT, "locationEnabled not implemented in Android");
    }

    @Kroll.setProperty
    public void setAsksForLocationPermissions(Object arg) {
        Log.w(LCAT, "aksForLocationPermissions not implemented in Android");
    }

    @Kroll.method
    public void hitTag(Object arg) {
        if (!registered) {
            Log.w(LCAT, "Please call registerForRemoteNotifications() first");
            return;
        }

        if (arg == null) {
            Log.w(LCAT, "Please provide tag");
            return;
        }

        String tag = TiConvert.toString(arg);
        pushConnector.hitTag(tag);
    }

    @Kroll.method
    public void hitImpression(Object arg) {
        if (!registered) {
            Log.w(LCAT, "Please call registerForRemoteNotifications() first");
            return;
        }

        if (arg == null) {
            Log.w(LCAT, "Please provide impression");
            return;
        }

        String tag = TiConvert.toString(arg);
        pushConnector.hitTag(tag);
    }

    @Kroll.method
    public void showPushList() {
        if (!registered) {
            Log.w(LCAT, "Please call registerForRemoteNotifications() first");
            return;
        }

        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        Intent intent = new Intent(appContext, XPushLogActivity.class);
        app.getCurrentActivity().startActivity(intent);
    }

    @Kroll.method
    public void getPushNotifications(@Kroll.argument(optional=true) Object args) {
        if (!registered) {
            Log.w(LCAT, "Please call registerForRemoteNotifications() first");
            return;
        }

        int offset = 0;
        int limit = Integer.MAX_VALUE;
        KrollFunction successCallback = null;
        if (args instanceof HashMap) {
            HashMap<String, Object> options = (HashMap<String, Object>) args;
            if (options.containsKey("offset")) offset = TiConvert.toInt(options, "offset");
            if (options.containsKey("limit")) limit = TiConvert.toInt(options, "limit");
            Object success = options.get("success");
            if (success instanceof KrollFunction) successCallback = (KrollFunction) success;
        }

        class Callback implements Handler.Callback {
            KrollFunction successCallback = null;
            KrollObject module = null;
            int offset = 0;
            int limit = 0;

            public Callback(KrollObject module, KrollFunction successCallback, int offset, int limit) {
                this.module = module;
                this.successCallback = successCallback;
                this.offset = offset;
                this.limit = limit;
            }

            @Subscribe
            public void consumeEventList(EventsPushlistWrapper pushMessageListItems) {
                PushConnector.unregisterInEventBus(this);
                ArrayList<PushmessageListItem> pushList = pushMessageListItems.getEventPushlist();

                ArrayList<HashMap> notifications = new ArrayList<HashMap>();
                for (PushmessageListItem item : pushList) {
                    HashMap<String, Object> notification = new KrollDict();
                    notification.put("id", item.id);
                    notification.put("messageId", item.messageId);
                    notification.put("read", item.read);
                    if (item.createTimestamp != null && !item.createTimestamp.equals(""))
                        notification.put("createTimestamp", TiConvert.toInt(item.createTimestamp));
                    if (item.locationId != null && !item.locationId.equals("null"))
                        notification.put("locationId", item.locationId);
                    if (item.tag != null && !item.tag.equals("null"))
                        notification.put("tag", item.tag);
                    PushMessage message = item.message;
                    if (message != null) {
                        notification.put("openInBrowser", message.openInBrowser);
                        if (message.alert != null) notification.put("alert", message.alert);
                        if (message.sound != null && !message.sound.equals(""))
                            notification.put("sound", message.sound);
                        if (message.url != null && !message.url.equals(""))
                            notification.put("url", message.url);
                        if (message.badge != null && !message.badge.equals(""))
                            notification.put("badge", TiConvert.toInt(message.badge));
                        if (message.pushActionId != null && !message.pushActionId.equals("null"))
                            notification.put("pushActionId", message.pushActionId);
                    }
                    notifications.add(notification);
                }

                HashMap<String, Object> res = new HashMap<String, Object>();
                res.put("code", 0);
                res.put("success", true);
                res.put("notifications", notifications.toArray());
                successCallback.call(module, res);
            }

            @Override
            public boolean handleMessage(Message message) {
                PushConnector.registerInEventBus(this);
                pushConnector.getPushlist(offset, limit);
                return true;
            }
    }

        Callback callback = new Callback(getKrollObject(), successCallback, offset, limit);
        new Handler(Looper.getMainLooper(), callback).obtainMessage(0).sendToTarget();
    }

    private void initNotificationMessageReceivers(){
        IntentFilter intentFilter = new IntentFilter(
                "ie.imobile.extremepush.action_message");

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                if (extras != null)
                {
                    for (String key : extras.keySet()) {
                        Object value = extras.get(key);
//                        Log.e(LCAT, kevaluey);
                    }
//                    if (inForeground) {
//                        extras.putBoolean("foreground", true);
//                        sendExtras(extras);
//                    }
                }
            }
        };

        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        appContext.registerReceiver(mReceiver, intentFilter);
    }
}