//
//  UICustomPickerController.m
//  My happy client
//
//  Created by Adriela on 30/01/2020.
//
#import "UICustomPickerController.h"

@implementation UICustomPickerController
-(void)initData:(UIImageView*)souche mCamera:(ScoplanCamera*)mcm{
    imageSouche = souche;
    mCamera = mcm;
    UITapGestureRecognizer *tapGesture1 = [[UITapGestureRecognizer alloc] initWithTarget:self  action:@selector(launchDSPhotoEditorSDK:)];
    tapGesture1.numberOfTapsRequired = 1;
    imageSouche.userInteractionEnabled = YES;
    [tapGesture1 setDelegate:self];
    [imageSouche addGestureRecognizer:tapGesture1];
    [mCamera addDrawSel:@selector(launchDSPhotoEditorSDK:)];
}
/* DSPphoto */
- (void) launchDSPhotoEditorSDK: (id) sender {
    NSArray *toolsToHide = @[@(TOOL_ORIENTATION), @(TOOL_FRAME),@(TOOL_FILTER),@(TOOL_ROUND),@(TOOL_EXPOSURE), @(TOOL_CIRCLE),@(TOOL_CONTRAST),@(TOOL_VIGNETTE),@(TOOL_SATURATION),@(TOOL_SHARPNESS),@(TOOL_WARMTH),@(TOOL_PIXELATE),@(TOOL_CROP),@(TOOL_DRAW),@(TOOL_TEXT),@(TOOL_STICKER)];
    dsController = [[DSPhotoEditorViewController alloc] initWithImage:imageSouche.image toolsToHide:toolsToHide];
    NSArray *colorImages =@[@"measure_0",@"measure_1",@"measure_2",@"measure_3",@"measure_4",@"measure_5",@"measure_6",@"measure_7",@"measure_8",@"measure_9",@"measure_10",@"measure_11"];
    [dsController setCustomStickers:colorImages inBundle:[NSBundle mainBundle]];
    dsController.delegate = self;
    dsController.modalPresentationStyle = UIModalPresentationFullScreen;
    [self presentViewController:dsController animated:YES completion:^(){
        
    }];
    UIView* parentBottom = [self->dsController dsBottomScrollView];
    if([[self->dsController dsBottomContentView].superview viewWithTag:46] == nil){
        dispatch_async( dispatch_get_main_queue(), ^{
            UIView* btnView = [[[NSBundle mainBundle] loadNibNamed:@"bottomPhotoEditor" owner:self->dsController options:nil] objectAtIndex:0];
            [self imageAddTarget:[btnView viewWithTag:10] sel:@selector(openCrop)];
            [self imageAddTarget:[btnView viewWithTag:20] sel:@selector(openDraw)];
            [self imageAddTarget:[btnView viewWithTag:30] sel:@selector(openText)];
            [self imageAddTarget:[btnView viewWithTag:40] sel:@selector(deleteImage)];
            [self imageAddTarget:[btnView viewWithTag:52] sel:@selector(openSticker)];
            [self->dsController.dsBottomScrollView setHidden:YES];
            btnView.layer.frame = CGRectMake(0, [self->dsController dsPhotoEditorImageView].layer.frame.size.height + [self->dsController dsPhotoEditorImageView].layer.frame.origin.y + ([UIScreen mainScreen].bounds.size.height - self->dsController.dsBottomContentView.layer.frame.size.height - ([self->dsController dsPhotoEditorImageView].layer.frame.size.height + [self->dsController dsPhotoEditorImageView].layer.frame.origin.y)), [UIScreen mainScreen].bounds.size.width, self->dsController.dsBottomContentView.layer.frame.size.height);
            [parentBottom.superview insertSubview:btnView belowSubview:[self->dsController dsBottomScrollView]];
            UIButton* cancel = [self->dsController.dsTopStackView subviews][0];
            [cancel addTarget:self action:@selector(switchBtn) forControlEvents:UIControlEventTouchUpInside];
            UIButton* ok = [self->dsController.dsTopStackView subviews][2];
                [ok addTarget:self action:@selector(switchBtn) forControlEvents:UIControlEventTouchUpInside];
        });
    }
}

-(void)imageAddTarget:(UIView*) img sel:(SEL)selector{
    UITapGestureRecognizer *tapG = [[UITapGestureRecognizer alloc] initWithTarget:self  action:selector];
    tapG.numberOfTapsRequired = 1;
    img.userInteractionEnabled = YES;
    [tapG setDelegate:self];
    [img addGestureRecognizer:tapG];
}

-(void)openSticker{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self->dsController.dsBottomScrollView setHidden:NO];
    });
    [dsController.dsTopTitleView setText:@"Mesure"];
    [dsController dsPhotoEditorSticker:dsController];
    [dsController.dsTopTitleView setText:@"Mesure"];
}

-(void)openCrop{
    [dsController dsPhotoEditorCrop:dsController];
}

-(void)openDraw{
    [dsController dsPhotoEditorDraw:dsController];
}

-(void)openText{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self->dsController.dsBottomScrollView setHidden:NO];
    });
    [dsController dsPhotoEditorStickerText:dsController];
}

-(void)switchBtn{
    dispatch_async(dispatch_get_main_queue(), ^{
        [self->dsController.dsBottomScrollView setHidden:YES];
    });
}

-(void)deleteImage{
    [dsController dsPhotoEditorCancel:nil];
    [mCamera removeLastPreview];
}
    
- (void)dsPhotoEditor:(DSPhotoEditorViewController *)editor finishedWithImage:(UIImage *)image {
    [self dismissViewControllerAnimated:YES completion:^(){
        NSDate *currentDate = [NSDate date];
        NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
        [dateFormatter setDateFormat:@"dd_MM_YY_HH_mm_ss_SSS"];
        NSString *filename = [dateFormatter stringFromDate:currentDate];
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *docsPath = [paths objectAtIndex:0];
        NSString* thumbnailPath = [NSString stringWithFormat:@"%@/%@_mthumb.jpg", docsPath, filename];
        NSError* err = nil;
        if ([UIImageJPEGRepresentation(image, 0.8) writeToFile:thumbnailPath options:NSAtomicWrite error:&err]){
            NSLog(@"%@",thumbnailPath);
        }else {
            if (err) {
                NSLog(@"Error saving image: %@", [err localizedDescription]);
            }
        }
        [self->mCamera flushPicture:thumbnailPath];
        [self->mCamera setPreview:image];
    }];
}
    
- (void)dsPhotoEditorCanceled:(DSPhotoEditorViewController *)editor {
    [self dismissViewControllerAnimated:YES completion:nil];
}
@end
