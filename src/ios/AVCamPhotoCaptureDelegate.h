//
//  AVCamPhotoCaptureDelegate.h
//  My happy client
//
//  Created by Adriela on 02/05/2018.
//

@import AVFoundation;
@class UIScoplanCamera;
@interface AVCamPhotoCaptureDelegate : NSObject<AVCapturePhotoCaptureDelegate>{
    UIScoplanCamera *mUicam;
}
- (void)setUICam:(UIScoplanCamera*)uicam;

- (instancetype)initWithRequestedPhotoSettings:(AVCapturePhotoSettings *)requestedPhotoSettings willCapturePhotoAnimation:(void (^)(void))willCapturePhotoAnimation livePhotoCaptureHandler:(void (^)( BOOL capturing ))livePhotoCaptureHandler completionHandler:(void (^)( AVCamPhotoCaptureDelegate *photoCaptureDelegate ))completionHandler;

- (NSData*) getPhotodata;
@property (nonatomic, readonly) AVCapturePhotoSettings *requestedPhotoSettings;

@end