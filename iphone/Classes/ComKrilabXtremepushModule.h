/**
 * XtremePush_Titanium
 *
 * Created by Your Name
 * Copyright (c) 2014 Your Company. All rights reserved.
 */

#import "TiModule.h"

@interface ComKrilabXtremepushModule : TiModule {
    KrollCallback *_registerSuccessCallback;
    KrollCallback *_registerErrorCallback;
}

@property(nonatomic, strong) KrollCallback *registerSuccessCallback;
@property(nonatomic, strong) KrollCallback *registerErrorCallback;
@end
