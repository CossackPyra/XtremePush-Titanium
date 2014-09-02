package com.krilab.xtremepush;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.support.v4.app.FragmentManager;
import com.google.android.gcm.GCMRegistrar;
import com.squareup.otto.Subscribe;
import ie.imobile.extremepush.GCMIntentService;
import ie.imobile.extremepush.PushConnector;
import ie.imobile.extremepush.api.model.EventsPushlistWrapper;
import ie.imobile.extremepush.api.model.PushMessage;
import ie.imobile.extremepush.api.model.PushmessageListItem;
import ie.imobile.extremepush.ui.XPushLogActivity;
import ie.imobile.extremepush.util.LibVersion;
import ie.imobile.extremepush.util.LocationAccessHelper;
import ie.imobile.extremepush.util.SharedPrefUtils;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollObject;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.*;
import org.appcelerator.titanium.util.TiActivityResultHandler;
import org.appcelerator.titanium.util.TiActivitySupportHelper;
import org.appcelerator.titanium.util.TiConvert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("unused")
@Kroll.module(name = "XtremePushTitanium", id = "com.krilab.xtremepush")
public class XtremePushTitaniumModule extends KrollModule {
    private static final String TAG = XtremePushTitaniumModule.class.getName();

    private PushConnector pushConnector;
    private boolean registered;
    private BroadcastReceiver messageReceiver;
    private BroadcastReceiver registerReceiver;
    KrollFunction receiveCallback;
    KrollFunction registerCallback;
    Intent savedMessage;


    @SuppressWarnings("unused")
    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {
        ie.imobile.extremepush.util.XR.init(app);
    }

    @SuppressWarnings("unused")
    @Kroll.method
    public void registerForRemoteNotifications(@Kroll.argument(optional = true) Object args) {
        if (registered) {
            Log.w(TAG, "registerForRemoteNotifications(): already registered; return");
            return;
        }
        if (pushConnector != null) {
            Log.w(TAG, "registerForRemoteNotifications(): already in registration process; return");
            return;
        }

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
            locationTimeoutValue = options.get("locationTimeout");
            locationDistanceValue = options.get("locationDistance");

            Object receiveCallback = options.get("callback");
            if (receiveCallback != null && !(receiveCallback instanceof KrollFunction)) {
                throw new IllegalArgumentException("registerForRemoteNotifications(): unsupported property type for 'callback' " + receiveCallback.getClass().getName());
            }
            this.receiveCallback = (KrollFunction) receiveCallback;

            Object registerCallback = options.get("success");
            if (registerCallback != null && !(registerCallback instanceof KrollFunction)) {
                throw new IllegalArgumentException("registerForRemoteNotifications(): unsupported property type for 'success' " + registerCallback.getClass().getName());
            }
            this.registerCallback = (KrollFunction) registerCallback;
        }

        initMessageReceiver();
        initLocationHandler();
        pushConnector = createPushConnector(appKey, projectNumber, locationTimeoutValue, locationDistanceValue);
    }

    @SuppressWarnings("unused")
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    @Kroll.method
    public void getPushNotifications(@Kroll.argument(optional = true) Object args) {
        if (pushConnector == null) {
            Log.w(TAG, "Please call registerForRemoteNotifications() first");
            return;
        }
        if (!registered) {
            Log.w(TAG, "Wait before device will be registered");
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
            Log.w(TAG, "No success callback to getPushNotifications(); return");
            return;
        }

        NotificationsCallback callback = new NotificationsCallback(getKrollObject(), successCallback, offset, limit);
        new Handler(Looper.getMainLooper(), callback).obtainMessage(0).sendToTarget();
    }

    @SuppressWarnings("unused")
    @Kroll.method
    public void unregisterForRemoteNotifications() {
        pushConnector = null;
        registered = false;
    }

    @SuppressWarnings("unused")
    @Kroll.getProperty
    public boolean getIsSandboxModeOn() {
        Log.w(TAG, "isSandboxModeOn not implemented in Android");
        return false;
    }

    @SuppressWarnings("unused")
    @Kroll.getProperty
    public String getVersion() {
        return LibVersion.VER;
    }

    @SuppressWarnings("unused")
    @Kroll.getProperty
    public boolean getShouldWipeBadgeNumber() {
        Log.w(TAG, "shouldWipeBadgeNumber not implemented in Android");
        return false;
    }

    @SuppressWarnings("unused")
    @Kroll.setProperty
    public void setShouldWipeBadgeNumber(Object arg) {
        Log.w(TAG, "shouldWipeBadgeNumber not implemented in Android");
    }

    @SuppressWarnings("unused")
    @Kroll.getProperty
    public HashMap getDeviceInfo() {
        if (pushConnector == null) {
            Log.w(TAG, "Please call registerForRemoteNotifications() first");
            return null;
        }
        if (!registered) {
            Log.w(TAG, "Wait before device will be registered");
            return null;
        }

        // @WARN: can crash if no deviceToken or XPushDeviceID (bug in android SDK)
        return pushConnector.getDeviceInfo();
    }

    @SuppressWarnings("unused")
    @Kroll.setProperty
    public void setLocationEnabled(Object arg) {
        if (pushConnector == null) {
            Log.w(TAG, "Please call registerForRemoteNotifications() first");
            return;
        }
        if (!registered) {
            Log.w(TAG, "Wait before device will be registered");
            return;
        }

        boolean locationEnabled = TiConvert.toBoolean(arg);
        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        PushConnector.locationEnabled(appContext, locationEnabled);
    }

    @SuppressWarnings("unused")
    @Kroll.setProperty
    public void setAsksForLocationPermissions(Object arg) {
        if (arg == null) {
            Log.w(TAG, "setAsksForLocationPermissions(): please provide value");
            return;
        }

        boolean asksForLocationPermissions = TiConvert.toBoolean(arg, false);

        TiApplication app = TiApplication.getInstance();
        TiRootActivity rootActivity = app.getRootActivity();
        SharedPrefUtils.setPromptTurnLocation(rootActivity, asksForLocationPermissions);
    }

    @SuppressWarnings("unused")
    @Kroll.method
    public void hitTag(Object arg) {
        if (pushConnector == null) {
            Log.w(TAG, "Please call registerForRemoteNotifications() first");
            return;
        }
        if (!registered) {
            Log.w(TAG, "Wait before device will be registered");
            return;
        }
        if (arg == null) {
            Log.w(TAG, "Please provide tag");
            return;
        }

        String tag = TiConvert.toString(arg);
        pushConnector.hitTag(tag);
    }

    @SuppressWarnings("unused")
    @Kroll.method
    public void hitImpression(Object arg) {
        if (pushConnector == null) {
            Log.w(TAG, "Please call registerForRemoteNotifications() first");
            return;
        }
        if (!registered) {
            Log.w(TAG, "Wait before device will be registered");
            return;
        }
        if (arg == null) {
            Log.w(TAG, "Please provide impression");
            return;
        }

        String tag = TiConvert.toString(arg);
        pushConnector.hitImpression(tag);
    }

    @SuppressWarnings("unused")
    @Kroll.method
    public void showPushList() {
        if (pushConnector == null) {
            Log.w(TAG, "Please call registerForRemoteNotifications() first");
            return;
        }
        if (!registered) {
            Log.w(TAG, "Wait before device will be registered");
            return;
        }

        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        Intent intent = new Intent(appContext, XPushLogActivity.class);
        app.getCurrentActivity().startActivity(intent);
    }

    @Kroll.setProperty @Kroll.method
    public void setReceiveCallback(Object arg) {
        if (arg == null) {
            Log.w(TAG, "Please provide callback");
            return;
        }
        if (!(arg instanceof KrollFunction)) {
            throw new IllegalArgumentException("Unsupported property type for receiveCallback " + arg.getClass().getName());
        }

        this.receiveCallback = (KrollFunction) arg;
    }


    private void setRegistered() {
        registered = true;
        if (registerCallback != null) {
            String deviceToken = pushConnector.getDeviceInfo().get("deviceToken");

            HashMap<String, Object> res = new HashMap<String, Object>();
            res.put("success", true);
            res.put("code", 0);
            res.put("deviceToken", deviceToken);
            registerCallback.call(getKrollObject(), res);
        }

        fireSavedMessage();
    }

    private PushConnector createPushConnector(String appKey, String projectNumber, Object locationTimeoutValue, Object locationDistanceValue) {
        TiApplication app = TiApplication.getInstance();
        TiBaseActivity activity = app.getRootActivity();
        FragmentManager fragmentManager = activity.getSupportFragmentManager();

        PushConnector pushConnector;
        if (locationTimeoutValue != null && locationDistanceValue != null) {
            int locationTimeout = TiConvert.toInt(locationTimeoutValue);
            int locationDistance = TiConvert.toInt(locationDistanceValue);
            pushConnector = PushConnector.init(fragmentManager, appKey, projectNumber, locationTimeout, locationDistance);
        } else {
            pushConnector = PushConnector.init(fragmentManager, appKey, projectNumber);
        }

        class TransactionExecutor implements Runnable {
            FragmentManager fragmentManager;
            public TransactionExecutor(FragmentManager fragmentManager) {
                this.fragmentManager = fragmentManager;
            }

            @Override
            public void run() {
                // complete transaction to be assure PushManager in PushConnector created
                fragmentManager.executePendingTransactions();

                TiApplication app = TiApplication.getInstance();
                if (!GCMRegistrar.isRegistered(app)) {
                    initRegisterReceiver();
                } else {
                    setRegistered();
                }
            }
        }
        activity.runOnUiThread(new TransactionExecutor(fragmentManager));

        return pushConnector;
    }


    class StartLocationHandler implements TiActivityResultHandler {
        @Override
        public void onResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (pushConnector == null) return;
            pushConnector.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        public void onError(Activity activity, int requestCode, Exception e) {
            Log.e(TAG, "StartLocationHandler error");
        }
    }

    private void initLocationHandler() {
        TiApplication app = TiApplication.getInstance();
        TiBaseActivity rootActivity = app.getRootActivity();

        // getSupportHelper() is protected so try to search it to register START_LOCATION_ACTIVITY_CODE
        rootActivity.getUniqueResultCode(); // init support helper
        for (int i = 0; i < 100; i++) { // search rootActivity helper; must be in first id's
            TiActivitySupportHelper activitySupportHelper = TiActivitySupportHelpers.retrieveSupportHelper(rootActivity, i);
            if (activitySupportHelper == null) continue;

            StartLocationHandler startLocationHandler = new StartLocationHandler();
            int locationActivityCode = LocationAccessHelper.START_LOCATION_ACTIVITY_CODE;
            activitySupportHelper.registerResultHandler(locationActivityCode, startLocationHandler);
        }
    }


    private void initRegisterReceiver() {
        if (registerReceiver != null) return;
        registerReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setRegistered();
            }
        };

        IntentFilter intentFilter = new IntentFilter(GCMIntentService.ACTION_REGISTER_ON_SERVER);
        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        appContext.registerReceiver(registerReceiver, intentFilter);
    }

    private void initMessageReceiver() {
        if (messageReceiver != null) return;
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                fireReceive(intent);
            }
        };

        IntentFilter intentFilter = new IntentFilter(GCMIntentService.ACTION_MESSAGE);
        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        appContext.registerReceiver(messageReceiver, intentFilter);
    }

    private void removeMessageReceiver() {
        if (messageReceiver == null) return;
        TiApplication app = TiApplication.getInstance();
        Context appContext = app.getApplicationContext();
        appContext.unregisterReceiver(messageReceiver);
    }

    private void fireReceive(Intent intent) {
        if (intent == null) return;

        Bundle extras = intent.getExtras();
        if (extras == null || receiveCallback == null) return;

        PushMessage message = (PushMessage) extras.get(GCMIntentService.EXTRAS_PUSH_MESSAGE);
        if (message == null) return;

        boolean inForeground = TiApplication.isCurrentActivityInForeground();
        boolean inBackground = extras.getBoolean(GCMIntentService.EXTRAS_FROM_NOTIFICATION, !inForeground);
        Map notification = notificationWithPushMessage(message);

        HashMap<String, Object> res = new HashMap<String, Object>();
        res.put("data", notification);
        res.put("inBackground", inBackground);

        Log.d(TAG, "Received notification inBackground=" + inBackground);
        receiveCallback.call(getKrollObject(), res);
    }

    private void fireSavedMessage() {
        if (savedMessage != null) {
            Log.d(TAG, "Fire saved message");
            fireReceive(savedMessage);
            savedMessage = null;
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
        public void consumeEventList(Object list) {
            if (!(list instanceof EventsPushlistWrapper)) return;
            EventsPushlistWrapper pushMessageListItems = (EventsPushlistWrapper) list;

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
        notification.putAll(message.extra);
        return notification;
    }

    @Override
    public void onStart(Activity activity) {
        super.onStart(activity);

        TiApplication app = TiApplication.getInstance();
        TiRootActivity rootActivity = app.getRootActivity();
        if (rootActivity == null) return;

        Intent intent = rootActivity.getIntent();
        if (intent ==  null) return;

        Bundle extras = intent.getExtras();
        if (extras == null) return;
        if (!extras.containsKey(GCMIntentService.EXTRAS_PUSH_MESSAGE)) return;

        savedMessage = intent;
        if (pushConnector != null)
            pushConnector.onNewIntent(intent);
    }

    @Override
    public void onDestroy(Activity activity) {
        super.onDestroy(activity);
        removeMessageReceiver();
    }
}