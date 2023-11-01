//
//  UIImagePickerDelegate.h
//  My happy client
//
//  Created by Adriela on 08/10/2018.
//
@class ScoplanCamera;
@interface UIImagePickerDelegate : NSObject<UIImagePickerControllerDelegate,UINavigationControllerDelegate>{
}
- (void) setCam:(CameraMultiple *) cam;
@end
