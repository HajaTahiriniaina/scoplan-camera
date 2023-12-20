#import <Cordova/CDV.h>
@class UIScoplanCamera;

@interface ScoplanCamera : CDVPlugin {
    UIScoplanCamera *mCamview;
    NSMutableArray *mpictures;
    CDVInvokedUrlCommand *mcallback;
}

- (void)takePictures:(CDVInvokedUrlCommand*)command;
- (void)insertPicture:(NSString*)url;
- (void)flushPicture:(NSString*)url;
- (void)dismissCam;
- (void)setPreview:(UIImage*)img;
- (void)addDrawSel:(SEL)selector;
- (void)resetAll;
- (void)removeLastPreview;
@end