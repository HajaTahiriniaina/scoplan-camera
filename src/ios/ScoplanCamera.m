#import "ScoplanCamera.h"
#import "UICameraMultiple.h"
#import "UIImagePickerDelegate.h"
#import "UICustomPickerController.h"
#import "CDVWKWebViewUIDelegate.h"

/********* ScoplanCamera.m Cordova Plugin Implementation *******/
@interface ScoplanCamera()
    @property (nonatomic)  UIImagePickerDelegate * pickerdelegate;
    @property (nonatomic) UIView * overLayView;
    @property (nonatomic) UICustomPickerController *cameraUI;
@end

@implementation ScoplanCamera

- (void) addDrawSel:(SEL)selector{
    UILabel* label = [self.cameraUI.cameraOverlayView viewWithTag:13];
    [label setUserInteractionEnabled:YES];
    UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self.cameraUI  action:selector];
    tap.numberOfTapsRequired = 1;
    [label addGestureRecognizer:tap];
    UIButton* btn = [self.cameraUI.cameraOverlayView viewWithTag:14];
    [btn addTarget:self.cameraUI action:selector forControlEvents:UIControlEventTouchUpInside];
}

- (void)insertPicture:(NSString*)url{
    UIView* view = [self.cameraUI.cameraOverlayView viewWithTag:11];
    [view setHidden:NO];
    [mpictures addObject:url];
}

- (void)flushPicture:(NSString*)url{
    NSUInteger count = [mpictures count];
    mpictures[count - 1] = url;
}

-(void)setPreview:(UIImage*)img{
    UIImageView * imagePreview = ((UIImageView *)[self.cameraUI.cameraOverlayView viewWithTag:3]);
    [imagePreview setImage:img];
}

-(void)resetAll{
    [mpictures removeAllObjects];
}

- (void)dismissCam{
    [self.cameraUI dismissViewControllerAnimated:TRUE completion:nil];
    CDVPluginResult* pluginResult = [CDVPluginResult
                                     resultWithStatus:CDVCommandStatus_OK
                                     messageAsArray:mpictures];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:mcallback.callbackId];
}

- (void) takePictures:(CDVInvokedUrlCommand*)command {
    [[UIDevice currentDevice] setValue:
     [NSNumber numberWithInteger: UIInterfaceOrientationPortrait]
                                forKey:@"orientation"];
    mcallback = command;
    mpictures = [[NSMutableArray alloc]init];
    [self.commandDelegate runInBackground: ^{
        CDVPluginResult* pluginResult = [CDVPluginResult
            resultWithStatus:CDVCommandStatus_NO_RESULT
                                         messageAsArray:self->mpictures];
        [pluginResult setKeepCallback:[[NSNumber alloc] initWithBool:TRUE]];
        self.pickerdelegate = [[UIImagePickerDelegate alloc]init];
        [self.pickerdelegate setCam:self];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        dispatch_async( dispatch_get_main_queue(), ^{
            NSBundle *nsbundle = [NSBundle mainBundle];
            NSArray * nib =  [nsbundle loadNibNamed:@"multicam" owner:self.viewController options:nil];
            self.overLayView = [nib objectAtIndex:0];
            UIButton *takeBtn = (UIButton *)[self.overLayView viewWithTag:1];
            [takeBtn addTarget: self action: @selector(takenClicked:) forControlEvents: UIControlEventTouchUpInside];
            UIButton *cancelBtn = (UIButton *)[self.overLayView viewWithTag:2];
            UIButton *cancelBtn2 = (UIButton *)[self.overLayView viewWithTag:12];
            dispatch_async( dispatch_get_main_queue(), ^{
                 [cancelBtn setTitle:@"Annuler" forState:UIControlStateNormal];
            });
            [cancelBtn addTarget: self action: @selector(cancelClicked:) forControlEvents: UIControlEventTouchUpInside];
            [cancelBtn2 addTarget: self action: @selector(cancelConfirm:) forControlEvents: UIControlEventTouchUpInside];
            UIImageView * imagePreview = ((UIImageView *)[self.overLayView viewWithTag:3]);
            imagePreview.image = nil;
            [self.webView addSubview:self.overLayView];
            [self startCameraControllerFromViewController:self.viewController usingDelegate:self->_pickerdelegate];
        } );
    }];
}

@end
