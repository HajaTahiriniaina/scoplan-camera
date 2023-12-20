#import "ScoplanCamera.h"
#import "UIScoplanCamera.h"
#import "UIImagePickerDelegate.h"
#import "UICustomPickerController.h"

/********* ScoplanCamera.m Cordova Plugin Implementation *******/
@interface ScoplanCamera()
    @property (nonatomic)  UIImagePickerDelegate * pickerdelegate;
    @property (nonatomic) UIView * overLayView;
    @property (nonatomic) UICustomPickerController *cameraUI;
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

-(void)takenClicked:(id)sender{
    NSLog(@"shoot");
    [self shoot];
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
    }
}

-(void)resetAll{
    [mpictures removeAllObjects];
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

- (void)dismisCam{
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
    self.cameraUI.cameraOverlayView = self.overLayView;
    float camHeight = ([[UIScreen mainScreen] bounds].size.height/2)*15/100;
    UIImageView * imagePreview = ((UIImageView *)[self.overLayView viewWithTag:3]);
    [self.cameraUI initData:imagePreview mCamera:self];
    self.cameraUI.cameraViewTransform = CGAffineTransformTranslate(self.cameraUI.cameraViewTransform, 0, camHeight);
    [controller presentViewController:self.cameraUI animated:YES completion:nil];
    return YES;
}

@end
