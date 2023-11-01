#import <DSPhotoEditorSDK/DSPhotoEditorSDK.h>
#import <DSPhotoEditorSDK/DSPhotoEditorViewController.h>
#import "ScoplanCamera.h"

@interface UICustomPickerController : UIImagePickerController<DSPhotoEditorViewControllerDelegate>{
    UIImageView* imageSouche;
    DSPhotoEditorViewController* dsController;
    ScoplanCamera* mCamera;
}
-(void)initData:(UIImageView*)souche mCamera:(ScoplanCamera*)mcm;
@end
