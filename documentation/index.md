# XtremePush Titanium Module

## Description

This module provide access to [XtremePush](http://xtremepush.com) service from Titanium applications.

## Install XtremePush module

Latest module version can be found [here](releases/latest).

### Mac OS X

Copy the distribution zip file into the `~/Library/Application Support/Titanium` folder

### Linux

Copy the distribution zip file into the `~/.titanium` folder

### Windows

Copy the distribution zip file into the `C:\ProgramData\Titanium` folder


### Config tiapp.xml

Import module:
```
<modules>
   ...
    <module version="1.0.0">com.krilab.xtremepush</module>
</modules>
```

Add XtremePush keys (replace 'XXXXXXXXXXXX' with your keys) :
```
<!-- remote notifications (android) -->
<property name="XtremePushApplicationKey" type="string">XXXXXXXXXXXX</property>
<property name="GoogleProjectNumber" type="string">XXXXXXXXXXXX</property>
<!-- remote notifications (android) -->

<ios>
    <plist>
        <dict>
            <!-- remote notifications (ios) -->
            <key>XtremePushApplicationKey</key>
            <string>XXXXXXXXXXXX</string>
            <key>XtremePushSandoxMode</key>
            <false/>
            <!-- remote notifications (ios) -->
        </dict>
    </plist>
</ios>
```

Add android services, receivers and activities (replace 'XXX.XXX.XXX' with your app id):
```
<android xmlns:android="http://schemas.android.com/apk/res/android">
    <manifest>
        ...
        <service android:name="ie.imobile.extremepush.GCMIntentService"/>
        <receiver android:name="ie.imobile.extremepush.GCMReceiver" android:permission="com.google.android.c2dm.permission.SEND">
            <intent-filter>
                <action android:name="com.google.android.c2dm.intent.RECEIVE"/>
                <action android:name="com.google.android.c2dm.intent.REGISTRATION"/>
                <category android:name="XXX.XXX.XXX"/>
            </intent-filter>
        </receiver>
        <receiver android:name="ie.imobile.extremepush.location.ProxymityAlertReceiver"/>
        <activity android:name="ie.imobile.extremepush.ui.WebViewActivity" android:exported="false" />
        <activity android:name="ie.imobile.extremepush.ui.XPushLogActivity" android:exported="false" />
    </manifest>
</android>
```

Add android permissions (replace 'XXX.XXX.XXX' with your app id 2 times):
```
<android xmlns:android="http://schemas.android.com/apk/res/android">
    <manifest>
        ...
        <uses-permission android:name="android.permission.INTERNET"/>
        <uses-permission android:name="XXX.XXX.XXX.permission.C2D_MESSAGE"/>
        <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE"/>
        <uses-permission android:name="android.permission.GET_ACCOUNTS"/>
        <uses-permission android:name="android.permission.WAKE_LOCK"/>
        <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
        <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
        <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
        <permission android:name="XXX.XXX.XXX.permission.C2D_MESSAGE" android:protectionLevel="signature"/>
    </manifest>
</android>
```


## Using XtremePush Module

To access this module from JavaScript, you would do the following:

    var xtremepush = require("com.krilab.xtremepush");

The xtremepush variable is a reference to the Module object.


## Reference

### Functions

#### xtremepush.registerForRemoteNotifications(options)

On Android this function **MUST BE** called before any other actions!

Register device to receive remote notifications.

```
xtremepush.registerForRemoteNotifications({
    success: function (e) {
        var success = e.success; // always true
        var errorCode = e.code; // always 0
        var deviceToken = e.deviceToken;
    },
    error: function (e) { // only iOS
        var success = e.success; // always false
        var errorCode = e.code;
        var error = e.error; // localized error message
    },
    callback: function (e) { // receive callback; also can be set before or after registration by receiveCallback property
       var inBackground = e.inBackground; // notification was receive in background?
       var data = e.data; // notification data
    },
    
    // iOS specific; no effect for Android
    types: [
        Ti.Network.NOTIFICATION_TYPE_ALERT,
        Ti.Network.NOTIFICATION_TYPE_BADGE,
        Ti.Network.NOTIFICATION_TYPE_SOUND
    ],
    
    showAlerts: true,
    // Android specific; no effect for iOS
    locationTimeoutValue: 60,
    locationDistance: 2000
});
```

#### xtremepush.unregisterForRemoteNotifications()

Remove device registration.
On Android you **MUST NOT** use any action after this!

#### xtremepush.hitTag(tag)

```
xtremepush.hitTag("Main screen")
```

#### xtremepush.hitImpression(tag)

```
xtremepush.hitImpression("Big Bang!")
```

#### xtremepush.getPushNotifications(options)

Get received push notifications.

```
xtremepush.getPushNotifications({
    offset: 2, // default to 0
    limit: 5, // default to all
    success: function (e) {
        var success = e.success; // always true
        var errorCode = e.code; // always 0
        var notifications = e.notifications; // array of notifications
    }
});
```

#### xtremepush.showPushList()

Show push list (open new screen).


### Properties

#### xtremepush.isSandboxModeOn

iOS only

#### xtremepush.version

#### xtremepush.deviceInfo

#### xtremepush.shouldWipeBadgeNumber

iOS only

#### xtremepush.locationEnabled

#### xtremepush.asksForLocationPermissions
