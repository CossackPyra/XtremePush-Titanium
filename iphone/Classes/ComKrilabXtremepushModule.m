/**
* XtremePush_Titanium
*
* Created by Your Name
* Copyright (c) 2014 Your Company. All rights reserved.
*/

#import "ComKrilabXtremepushModule.h"
#import "TiHost.h"
#import "TiApp.h"
#import "XPush.h"

@implementation ComKrilabXtremepushModule

#pragma mark Internal

// this is generated for your module, please do not change it
- (id)moduleGUID {
    return @"5b919b68-52b9-4a7f-9c2c-fe95965794d3";
}

// this is generated for your module, please do not change it
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
    NSLog(@"[INFO] %@ loaded", [self moduleId]);
}

+ (void)applicationDidFinishLaunching:(NSNotification *)userInfo {
    NSDictionary *launchOptions = [TiApp app].launchOptions;
    [XPush applicationDidFinishLaunchingWithOptions:launchOptions];
}


#pragma Public APIs

- (id)register:(id)args {
    ENSURE_SINGLE_ARG(args, NSDictionary);
    NSArray *types = args[@"types"];
    NSNumber *showAlerts = args[@"showAlerts"];
    KrollCallback *success = args[@"success"];
    KrollCallback *error = args[@"error"];
    ENSURE_TYPE_OR_NIL(types, NSArray);
    ENSURE_TYPE_OR_NIL(showAlerts, NSNumber);
    ENSURE_TYPE_OR_NIL(success, KrollCallback);
    ENSURE_TYPE_OR_NIL(error, KrollCallback);

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

    [[TiApp app] setRemoteNotificationDelegate:self];
    [XPush registerForRemoteNotificationTypes:notificationTypes];

    return nil;
}

- (void)unregister {
    [XPush unregisterForRemoteNotifications];
}


#pragma mark Push Notification Delegates

- (void)application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken {
    [XPush applicationDidRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}

- (void)application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
    [XPush applicationDidFailToRegisterForRemoteNotificationsWithError:error];
}

@end
