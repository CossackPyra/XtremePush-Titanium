#import "TiModule.h"

@interface ComKrilabXtremepushModule : TiModule {
    KrollCallback *_registerSuccessCallback;
    KrollCallback *_registerErrorCallback;
    KrollCallback *_receiveCallback;
    BOOL _showAlerts;
}

@property(nonatomic, strong) KrollCallback *registerSuccessCallback;
@property(nonatomic, strong) KrollCallback *registerErrorCallback;
@property(nonatomic, strong) KrollCallback *receiveCallback;
@property(nonatomic) BOOL showAlerts;
@end