#import "ComKrilabXtremepushModule.h"
#import "TiHost.h"
#import "TiApp.h"
#import "XPush.h"

@implementation ComKrilabXtremepushModule


#pragma mark Internal

- (id)moduleGUID {
    return @"5b919b68-52b9-4a7f-9c2c-fe95965794d3";
}

- (NSString *)moduleId {
    return @"com.krilab.xtremepush";
}


#pragma mark Lifecycle

+ (void)load {
    NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
    [nc addObserver:self
           selector:@selector(applicationDidFinishLaunching:)
               name:@"UIApplicationDidFinishLaunchingNotification"
             object:nil];
}

- (void)startup {
    [super startup];

    [[TiApp app] setRemoteNotificationDelegate:self];
    NSLog(@"[INFO] %@ loaded", [self moduleId]);
}

+ (void)applicationDidFinishLaunching:(NSNotification *)userInfo {
    NSDictionary *launchOptions = [TiApp app].launchOptions;
    [XPush applicationDidFinishLaunchingWithOptions:launchOptions];
}


#pragma Public APIs

- (void)registerForRemoteNotifications:(id)args {
    ENSURE_SINGLE_ARG_OR_NIL(args, NSDictionary);
    NSArray *types = args[@"types"];
    NSNumber *showAlerts = args[@"showAlerts"];
    if (args[@"success"]) _registerSuccessCallback = args[@"success"];
    if (args[@"error"]) _registerErrorCallback = args[@"error"];
    if (args[@"callback"]) _receiveCallback = args[@"callback"];
    ENSURE_TYPE_OR_NIL(types, NSArray);
    ENSURE_TYPE_OR_NIL(showAlerts, NSNumber);
    ENSURE_TYPE_OR_NIL(_registerSuccessCallback, KrollCallback);
    ENSURE_TYPE_OR_NIL(_registerErrorCallback, KrollCallback);
    ENSURE_TYPE_OR_NIL(_receiveCallback, KrollCallback);

    _showAlerts = [TiUtils boolValue:showAlerts def:YES];

    UIRemoteNotificationType notificationTypes = UIRemoteNotificationTypeNone;
    for (NSNumber *type in types) {
        ENSURE_TYPE(type, NSNumber);
        switch ([type intValue]) {
            case 1: // NOTIFICATION_TYPE_BADGE
                notificationTypes |= UIRemoteNotificationTypeBadge;
                break;
            case 2: // NOTIFICATION_TYPE_ALERT
                notificationTypes |= UIRemoteNotificationTypeAlert;
                break;
            case 3: // NOTIFICATION_TYPE_SOUND
                notificationTypes |= UIRemoteNotificationTypeSound;
                break;
            case 4: // NOTIFICATION_TYPE_NEWSSTAND
                notificationTypes |= UIRemoteNotificationTypeNewsstandContentAvailability;
                break;
            default:
                break;
        }
    }
    if (notificationTypes == UIRemoteNotificationTypeNone)
        DebugLog(@"[WARN] XtremePush.register(): Push notification type is set to none");

    [[TiApp app] setRemoteNotificationDelegate:self];
    [XPush registerForRemoteNotificationTypes:notificationTypes];
}

- (void)unregisterForRemoteNotifications:(id)args {
    [XPush unregisterForRemoteNotifications];
}

- (id)isSandboxModeOn {
    return @([XPush isSandboxModeOn]);
}

- (id)version {
    return [XPush version];
}

- (id)deviceInfo {
    return [XPush deviceInfo];
}

- (id)shouldWipeBadgeNumber {
    return @([XPush shouldWipeBadgeNumber]);
}

- (void)setShouldWipeBadgeNumber:(id)value {
    BOOL wipeBadgeNumber = [TiUtils boolValue:value];
    [XPush setShouldWipeBadgeNumber:wipeBadgeNumber];
}

- (void)setLocationEnabled:(id)value {
    BOOL locationEnabled = [TiUtils boolValue:value];
    [XPush setLocationEnabled:locationEnabled];
}

- (void)setAsksForLocationPermissions:(id)value {
    BOOL asksForLocationPermissions = [TiUtils boolValue:value];
    [XPush setAsksForLocationPermissions:asksForLocationPermissions];
}

- (void)hitTag:(id)value {
    NSString *tag = [TiUtils stringValue:value];
    [XPush hitTag:tag];
}

- (void)hitImpression:(id)value {
    NSString *impression = [TiUtils stringValue:value];
    [XPush hitImpression:impression];
}

- (void)showPushList:(id)args {
    [XPush showPushListController];
}

- (void)getPushNotifications:(id)args {
    ENSURE_SINGLE_ARG_OR_NIL(args, NSDictionary);
    uint offset = (uint) [TiUtils intValue:@"offset" properties:args def:0];
    uint limit = (uint) [TiUtils intValue:@"limit" properties:args def:-1];
    KrollCallback *successCallback = args[@"success"];
    KrollCallback *errorCallback = args[@"errorCallback"];
    ENSURE_TYPE_OR_NIL(successCallback, KrollCallback);
    ENSURE_TYPE_OR_NIL(errorCallback, KrollCallback);

    [XPush getPushNotificationsOffset:offset limit:limit completion:^(NSArray *pushList, NSError *error) {
        if (error) {
            NSDictionary *res = @{
                    @"code" : @([error code]),
                    @"error" : [error localizedDescription],
                    @"success" : @NO
            };
            [self _fireEventToListener:@"notifications" withObject:res listener:errorCallback thisObject:self];
            return;
        }

        NSMutableArray *notifications = [NSMutableArray array];
        for (XPPushModel *model in pushList) {
            NSMutableDictionary *notification = [NSMutableDictionary dictionary];
            notification[@"badge"] = @(model.badge);
            notification[@"shouldOpenInApp"] = @(model.shouldOpenInApp);
            if (model.pushId) notification[@"pushId"] = model.pushId;
            if (model.locationId) notification[@"locationId"] = model.locationId;
            if (model.alert) notification[@"alert"] = model.alert;
            if (model.messageId) notification[@"messageId"] = model.messageId;
            if (model.url) notification[@"pushId"] = model.url;
            if (model.createDate)
                notification[@"createDate"] = [NSDateFormatter localizedStringFromDate:model.createDate
                                                                             dateStyle:NSDateFormatterShortStyle
                                                                             timeStyle:NSDateFormatterFullStyle];
            [notifications addObject:notification];
        }

        NSDictionary *res = @{
                @"code" : @0,
                @"success" : @YES,
                @"notifications" : notifications
        };
        [self _fireEventToListener:@"notifications" withObject:res listener:successCallback thisObject:self];
    }];
}


#pragma mark Push Notification Delegates

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    [XPush applicationDidRegisterForRemoteNotificationsWithDeviceToken:deviceToken];

    if (_registerSuccessCallback) {
        NSString *token = [[[[deviceToken description] stringByReplacingOccurrencesOfString:@"<" withString:@""]
                stringByReplacingOccurrencesOfString:@">" withString:@""]
                stringByReplacingOccurrencesOfString:@" " withString:@""];
        NSDictionary *res = @{
                @"code" : @0,
                @"success" : @YES,
                @"deviceToken" : token
        };
        [self _fireEventToListener:@"remote" withObject:res listener:_registerSuccessCallback thisObject:self];
    }
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    [XPush applicationDidFailToRegisterForRemoteNotificationsWithError:error];

    if (_registerErrorCallback) {
        NSDictionary *res = @{
                @"code" : @([error code]),
                @"error" : [error localizedDescription],
                @"success" : @NO
        };
        [self _fireEventToListener:@"remote" withObject:res listener:_registerErrorCallback thisObject:self];
    }
}

- (void)application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)userInfo {
    [XPush applicationDidReceiveRemoteNotification:userInfo showAlert:_showAlerts];
    if (_receiveCallback) {
        BOOL inBackground = (application.applicationState != UIApplicationStateActive);
        NSDictionary *res = @{
            @"data" : userInfo,
            @"inBackground" : @(inBackground)
        };
        [self _fireEventToListener:@"remote" withObject:res listener:_receiveCallback thisObject:self];
    }
}

@end
