package com.krilab.xtremepush;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.support.v4.app.FragmentManager;
import com.squareup.otto.Subscribe;
import ie.imobile.extremepush.GCMIntentService;
import ie.imobile.extremepush.util.LibVersion;
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

    private PushConnector pushConnector;
    private boolean registered;
    private boolean messageReceiverRegistered;
    private boolean registerReceiverRegistered;
    private boolean inBackground;
    KrollFunction receiveCallback;


    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {
        ie.imobile.extremepush.util.XR.init(app);
    }

    @Kroll.method
    public void registerForRemoteNotifications(@Kroll.argument(optional = true) Object args) {
        TiApplication app = TiApplication.getInstance();
        TiProperties properties = app.getAppProperties();

        String appKey = properties.getString("XtremePushApplicationKey", null);
        String projectNumber = properties.getString("GoogleProjectNumber", null);
        if (appKey == null || projectNumber == null) {
            throw new IllegalArgumentException("XtremePushApplicationKey or GoogleProjectNumber missed in tiapp.xml");
        }

        if (args != null && !(args instanceof HashMap)) {
            throw new IllegalArgumentException("registerForRemoteNotifications(): unsupported argument " + args.getClass().getName());
        }

        Object locationTimeoutValue = null;
        Object locationDistanceValue = null;
        if (args != null) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> options = (HashMap<String, Object>) args;
            locationTimeoutValue = options.get("locationTimeoutValue");
            locationDistanceValue = options.get("locationDistanceValue");
            Object receiveCallback = options.get("callback");
            if (receiveCallback != null && !(receiveCallback instanceof KrollFunction)) {
                throw new IllegalArgumentException("registerForRemoteNotifications(): unsupported property type for 'callback' " + receiveCallback.getClass().getName());
            }
            this.receiveCallback = (KrollFunction) receiveCallback;
        }

        TiRootActivity activity = app.getRootActivity();
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        ActivityProxy activityProxy = activity.getActivityProxy();
        activityProxy.addEventListener(TiC.EVENT_NEW_INTENT, new NewIntentCallback());

        // getSupportHelper() is protected so try to search it to register START_LOCATION_ACTIVITY_CODE
        activity.getUniqueResultCode(); // init support helper
        for (int i = 0; i < 100; i++) { // search activity helper
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

        initRegisterReceiver();
        initMessageReceiver();
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Kroll.method
    public void getPushNotifications(@Kroll.argument(optional = true) Object args) {
        if (!registered) {
            Log.w(LCAT, "Please call registerForRemoteNotifications() first");
            return;
        }

        if (args != null && !(args instanceof HashMap)) {
            throw new IllegalArgumentException("getPushNotifications(): unsupported argument " + args.getClass().getName());
        }

        int offset = 0;
        int limit = Integer.MAX_VALUE;
        KrollFunction successCallback = null;
        if (args != null) {
            @SuppressWarnings("unchecked")
            HashMap<String, Object> options = (HashMap<String, Object>) args;

            if (options.containsKey("offset")) offset = TiConvert.toInt(options, "offset");
            if (options.containsKey("limit")) limit = TiConvert.toInt(options, "limit");
            Object success = options.get("success");
            if (success != null && !(success instanceof KrollFunction)) {
                throw new IllegalArgumentException("Unsupported property type for 'success' " + success.getClass().getName());
            }
            successCallback = (KrollFunction) success;
        }
        if (successCallback == null) {
            Log.w(LCAT, "No success callback to getPushNotifications(); return");
            return;
        }

        NotificationsCallback callback = new NotificationsCallback(getKrollObject(), successCallback, offset, limit);
        new Handler(Looper.getMainLooper(), callback).obtainMessage(0).sendToTarget();
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
    public String getVersion() {
        return LibVersion.VER;
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

        // @WARN: can crash if no deviceToken or XPushDeviceID (bug in android SDK)
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

    private void initRegisterReceiver() {
        if (registerReceiverRegistered) return;

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                registered = true;
            }
        };

        IntentFilter intentFilter = new IntentFilter(GCMIntentService.ACTION_REGISTER_ON_SERVER);
        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        appContext.registerReceiver(mReceiver, intentFilter);

        registerReceiverRegistered = true;
    }

    private void initMessageReceiver() {
        if (messageReceiverRegistered) return;

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();
                if (extras == null || receiveCallback == null) return;

                PushMessage message = (PushMessage) extras.get(GCMIntentService.EXTRAS_PUSH_MESSAGE);
                Map notification = notificationWithPushMessage(message);

                HashMap<String, Object> res = new HashMap<String, Object>();
                res.put("data", notification);
                res.put("inBackground", inBackground);

                receiveCallback.call(getKrollObject(), res);
            }
        };

        IntentFilter intentFilter = new IntentFilter(GCMIntentService.ACTION_MESSAGE);
        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        appContext.registerReceiver(mReceiver, intentFilter);

        messageReceiverRegistered = true;
    }

    class NewIntentCallback implements KrollEventCallback {
        @Override
        public void call(Object o) {
            Log.e(LCAT, "new intent callback");

            KrollDict data = (KrollDict) o;
            IntentProxy ip = (IntentProxy) data.get(TiC.PROPERTY_INTENT);
            Intent intent = ip.getIntent();
            pushConnector.onNewIntent(intent);
        }
    }

    class StartLocationHandler implements TiActivityResultHandler {
        @Override
        public void onResult(Activity activity, int requestCode, int resultCode, Intent data) {
            Log.e(LCAT, "start location callback");

            pushConnector.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        public void onError(Activity activity, int requestCode, Exception e) {
            Log.e(LCAT, "start location error");
        }
    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    class NotificationsCallback implements Handler.Callback {
        KrollFunction successCallback;
        KrollObject module;
        int offset;
        int limit;

        public NotificationsCallback(KrollObject module, KrollFunction successCallback, int offset, int limit) {
            this.module = module;
            this.successCallback = successCallback;
            this.offset = offset;
            this.limit = limit;
        }

        @Subscribe
        public void consumeEventList(EventsPushlistWrapper pushMessageListItems) {
            PushConnector.unregisterInEventBus(this);
            if (successCallback == null) return;

            ArrayList<PushmessageListItem> pushList = pushMessageListItems.getEventPushlist();

            ArrayList<HashMap> notifications = new ArrayList<HashMap>();
            for (PushmessageListItem item : pushList) {
                HashMap<String, Object> notification = notificationWithPushMessage(item.message);

                notification.put("id", item.id);
                notification.put("messageId", item.messageId);
                notification.put("read", item.read);
                if (item.createTimestamp != null && !item.createTimestamp.equals("") && !item.createTimestamp.equals("")) {
                    notification.put("createTimestamp", TiConvert.toInt(item.createTimestamp));
                }
                if (item.locationId != null && !item.locationId.equals("") && !item.locationId.equals("null")) {
                    notification.put("locationId", item.locationId);
                }
                if (item.tag != null && !item.tag.equals("") && !item.tag.equals("null")) {
                    notification.put("tag", item.tag);
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

    private HashMap<String, Object> notificationWithPushMessage(PushMessage message) {
        HashMap<String, Object> notification = new HashMap<String, Object>();
        if (message == null) return notification;

        notification.put("openInBrowser", message.openInBrowser);
        if (message.alert != null) {
            notification.put("alert", message.alert);
        }
        if (message.sound != null && !message.sound.equals("") && !message.sound.equals("null")) {
            notification.put("sound", message.sound);
        }
        if (message.url != null && !message.url.equals("") && !message.url.equals("null")) {
            notification.put("url", message.url);
        }
        if (message.badge != null && !message.badge.equals("") && !message.badge.equals("null")) {
            notification.put("badge", TiConvert.toInt(message.badge));
        }
        if (message.pushActionId != null && !message.pushActionId.equals("") && !message.pushActionId.equals("null")) {
            notification.put("pushActionId", message.pushActionId);
        }

        return notification;
    }

    @Override
    public void onPause(Activity activity) {
        super.onPause(activity);
        inBackground = true;
    }

    @Override
    public void onResume(Activity activity) {
        super.onResume(activity);
        inBackground = false;
    }

    @Override
    public void onDestroy(Activity activity) {
        super.onDestroy(activity);
        inBackground = true;
    }
}