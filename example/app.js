var xtremepush = require("com.krilab.xtremepush");


var test = function() {
    Ti.API.debug(xtremepush.isSandboxModeOn);
    Ti.API.debug(xtremepush.version);
    Ti.API.debug(xtremepush.deviceInfo);

    Ti.API.debug(xtremepush.shouldWipeBadgeNumber);
    xtremepush.shouldWipeBadgeNumber = false;
    Ti.API.debug(xtremepush.shouldWipeBadgeNumber);

    Ti.API.debug(xtremepush.locationEnabled);
    xtremepush.locationEnabled = false;
    Ti.API.debug(xtremepush.locationEnabled);

    Ti.API.debug(xtremepush.asksForLocationPermissions);
    xtremepush.asksForLocationPermissions = true;
    Ti.API.debug(xtremepush.asksForLocationPermissions);

    xtremepush.hitTag("Main screen");
    xtremepush.hitImpression("Big Bang!");

    xtremepush.getPushNotifications({
        offset: 0, //default to 0
        limit: 5, // default to all
        success: function (e) {
            var success = e.success; // always true
            var errorCode = e.code; // always 0
            var notifications = e.notifications; // array of notifications
            Ti.API.debug(notifications);
        },
        error: function(e) {
            var success = e.success; // always false
            var errorCode = e.code;
            var error = e.error; // localized error message

            Ti.API.debug("getPushNotifications() error: " + errorCode + " - " + error);
        }
    });

    xtremepush.showPushList();
};


var onRegister = function(e) {
    var success = e.success; // always true
    var errorCode = e.code; // always 0
    var deviceToken = e.deviceToken;

    Ti.API.debug("Successfully register with deviceToken " + deviceToken);

    test();
};

var onRegistrationError = function(e) {
    var success = e.success; // always false
    var errorCode = e.code;
    var error = e.error; // localized error message

    Ti.API.debug("Registration error: " + errorCode + " - " + error);
};

var onReceive = function(e) {
    var inBackground = e.inBackgroud; // notification was receive in background?
    var notification = e.data;

    Ti.API.debug("Receive new notification inBackground=" + inBackground + ": " + notification.alert);
};

xtremepush.registerForRemoteNotifications({
    success: onRegister,
    error: onRegistrationError, // only iOS
    callback: onReceive,

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