#import "ScoplanCamera.h"
#import "UIScoplanCamera.h"
#import "UIImagePickerDelegate.h"
#import "UICustomPickerController.h"

/********* ScoplanCamera.m Cordova Plugin Implementation *******/
@interface ScoplanCamera()
    @property (nonatomic)  UIImagePickerDelegate * pickerdelegate;
    @property (nonatomic) UIView * overLayView;
    @property (nonatomic) UICustomPickerController *cameraUI;
    @property int photoLimit;
    @property int currentCount;
@end

@implementation ScoplanCamera

- (void)addDrawSel:(SEL)selector{
    UILabel* label = [self.cameraUI.cameraOverlayView viewWithTag:13];
    [label setUserInteractionEnabled:YES];
    UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self.cameraUI  action:selector];
    tap.numberOfTapsRequired = 1;
    [label addGestureRecognizer:tap];
    UIButton* btn = [self.cameraUI.cameraOverlayView viewWithTag:14];
    [btn addTarget:self.cameraUI action:selector forControlEvents:UIControlEventTouchUpInside];
}

-(void)cancelConfirm:(id)sender{
    NSLog(@"cancelOrok");
    UIAlertController* alert = [UIAlertController alertControllerWithTitle:@"Attention" message:@"Voulez-vous sortir sans enregistrer la photo" preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction* nonAction = [UIAlertAction actionWithTitle:@"Non" style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
        [alert dismissViewControllerAnimated:YES completion:nil];
    }];
    [alert addAction:nonAction];
    UIAlertAction* okAction = [UIAlertAction actionWithTitle:@"Oui" style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
        [alert dismissViewControllerAnimated:YES completion:nil];
        [mpictures removeAllObjects];
        [self dismisCam];
    }];
    [alert addAction:okAction];
    [self.cameraUI presentViewController:alert animated:YES completion:nil];
}

-(void)cancelClicked:(id)sender{
    NSLog(@"cancelOrok");
    [self dismisCam];
}

- (void) ShowAlert:(NSString *)Message {
    UIAlertController* alert = [UIAlertController alertControllerWithTitle:@"Attention" message:Message preferredStyle:UIAlertControllerStyleAlert];
    UIAlertAction* okAction = [UIAlertAction actionWithTitle:@"Ok" style:UIAlertActionStyleDefault handler:^(UIAlertAction * action) {
        [alert dismissViewControllerAnimated:YES completion:nil];
        [self dismisCam];
    }];
    [alert addAction:okAction];
    [self.cameraUI presentViewController:alert animated:YES completion:nil];
}

-(void)takenClicked:(id)sender{
    if(mpictures.count + self.currentCount == self.photoLimit) {
        [self ShowAlert:[NSString stringWithFormat:@"La prise de photos est limitée à %d par envoi", self.photoLimit]];
    } else {
        [self shoot];
    }
}

-(void)setPreview:(UIImage*)img{
    UIImageView * imagePreview = ((UIImageView *)[self.cameraUI.cameraOverlayView viewWithTag:3]);
    [imagePreview setImage:img];
}

-(void)removeLastPreview{
    UIImageView * imagePreview = ((UIImageView *)[self.cameraUI.cameraOverlayView viewWithTag:3]);
    if([mpictures count] > 1){
        [mpictures removeLastObject];
        UIImage *img = [UIImage imageWithContentsOfFile:[mpictures lastObject]];
        [imagePreview setImage:img];
    }else{
        [mpictures removeAllObjects];
        [imagePreview setImage:nil];
        UIView* view = [self.cameraUI.cameraOverlayView viewWithTag:11];
        [view setHidden:YES];
        // No more photos: button back to "X"
        UIButton* cancelBtn = (UIButton *)[self.cameraUI.cameraOverlayView viewWithTag:2];
        dispatch_async(dispatch_get_main_queue(), ^{
            [cancelBtn setTitle:@"X" forState:UIControlStateNormal];
        });
    }
}

-(void)resetAll{
    [mpictures removeAllObjects];
}

- (void)insertPicture:(NSString*)url{
    UIView* topBar = [self.cameraUI.cameraOverlayView viewWithTag:11];
    [mpictures addObject:url];
    dispatch_async(dispatch_get_main_queue(), ^{
        UIButton* cancelBtn = (UIButton *)[self.cameraUI.cameraOverlayView viewWithTag:2];
        [cancelBtn setTitle:@"OK" forState:UIControlStateNormal];

        // Position top bar below safe area
        CGFloat safeTop = 0;
        if (@available(iOS 11.0, *)) {
            safeTop = self.cameraUI.view.safeAreaInsets.top;
        }
        if (safeTop < 20) safeTop = 20;
        CGFloat screenWidth = [[UIScreen mainScreen] bounds].size.width;
        CGFloat barHeight = 44.0;
        topBar.frame = CGRectMake(0, safeTop, screenWidth, barHeight);
        [topBar setHidden:NO];
    });
}

- (void)flushPicture:(NSString*)url{
    NSUInteger count = [mpictures count];
    mpictures[count - 1] = url;
}

- (void)dismisCam{
    [self.cameraUI dismissViewControllerAnimated:TRUE completion:nil];
    CDVPluginResult* pluginResult = [CDVPluginResult
                                     resultWithStatus:CDVCommandStatus_OK
                                     messageAsArray:mpictures];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:mcallback.callbackId];
}

- (void) takePictures:(CDVInvokedUrlCommand*)command {
    NSDictionary *options = [command.arguments objectAtIndex:0];
    self.photoLimit = [options[@"photoLimit"] intValue];
    NSDictionary *options2 = [command.arguments objectAtIndex:1];
    self.currentCount = [options2[@"currentCount"] intValue];
//    NSLog([NSString stringWithFormat:@"photoLimit : %d", self.photoLimit]);
//    NSLog([NSString stringWithFormat:@"currentCount : %d", self.currentCount]);
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
                 [cancelBtn setTitle:@"X" forState:UIControlStateNormal];
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

- (void) shoot{
    [self.cameraUI takePicture];
}

- (BOOL) startCameraControllerFromViewController: (UIViewController*) controller
                                   usingDelegate: (id <UIImagePickerControllerDelegate,
                                                   UINavigationControllerDelegate>) delegate {
    
    if (([UIImagePickerController isSourceTypeAvailable:
          UIImagePickerControllerSourceTypeCamera] == NO)
        || (delegate == nil)
        || (controller == nil))
        return NO;
    
    self.cameraUI = [[UICustomPickerController alloc] init];
    self.cameraUI.sourceType = UIImagePickerControllerSourceTypeCamera;
    self.cameraUI.showsCameraControls = NO;
    self.cameraUI.cameraCaptureMode = UIImagePickerControllerCameraCaptureModePhoto;
    self.cameraUI.allowsEditing = NO;
    self.cameraUI.delegate = delegate;
    self.overLayView.frame = self.cameraUI.cameraOverlayView.frame;
    self.overLayView.backgroundColor = [UIColor clearColor];
    self.cameraUI.cameraOverlayView = self.overLayView;
    UIImageView * imagePreview = ((UIImageView *)[self.overLayView viewWithTag:3]);
    [self.cameraUI initData:imagePreview mCamera:self];

    // Scale camera preview to fill from safe-area-top to bottom of screen
    CGFloat screenHeight = [[UIScreen mainScreen] bounds].size.height;
    CGFloat screenWidth  = [[UIScreen mainScreen] bounds].size.width;
    CGFloat safeTop = 0;
    if (@available(iOS 11.0, *)) {
        UIWindow *window = UIApplication.sharedApplication.keyWindow;
        if (window) safeTop = window.safeAreaInsets.top;
    }
    if (safeTop < 20) safeTop = 20;
    CGFloat visibleHeight = screenHeight - safeTop;
    CGFloat cameraPreviewHeight = screenWidth * 4.0 / 3.0;
    CGFloat scale = visibleHeight / cameraPreviewHeight;
    if (scale < 1.0) scale = 1.0;
    // Translate the scaled preview down so its top aligns with safe-area-top
    CGFloat translateY = safeTop / 2.0 + (visibleHeight - cameraPreviewHeight) / 2.0;
    CGAffineTransform scaleTransform = CGAffineTransformMakeScale(scale, scale);
    CGAffineTransform translateTransform = CGAffineTransformMakeTranslation(0, translateY);
    self.cameraUI.cameraViewTransform = CGAffineTransformConcat(scaleTransform, translateTransform);

    // Add a black bar covering the safe-area-top (behind status bar)
    UIView *statusBarBg = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screenWidth, safeTop)];
    statusBarBg.backgroundColor = [UIColor blackColor];
    statusBarBg.tag = 98;
    [self.overLayView addSubview:statusBarBg];
    [self.overLayView sendSubviewToBack:statusBarBg];

    // Switch top bar (tag=11) to frame-based positioning so we can place it below safe area
    UIView *topBar = [self.overLayView viewWithTag:11];
    if (topBar) {
        topBar.translatesAutoresizingMaskIntoConstraints = YES;
        topBar.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleBottomMargin;
    }

    [controller presentViewController:self.cameraUI animated:YES completion:nil];
    return YES;
}

@end
