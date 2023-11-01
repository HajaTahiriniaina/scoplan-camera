//
//  UIScoplanCamera.h
//  My happy client
//
//  Created by Adriela on 30/04/2018.
//

@import UIKit;
@class CMMotionManager;
@class ScoplanCamera;
@class AVCamPhotoCaptureDelegate;
@interface UIScoplanCamera : UIViewController {
    ScoplanCamera *mCamera;
    CMMotionManager *cm;
}
- (void)insertImgUrl:(NSString*)url;
- (void)setMCam:(ScoplanCamera*)mCam;
- (void) setPhotodata:(AVCamPhotoCaptureDelegate*)photoCaptureDelegate;
- (NSInteger)getOrientation;
@end
