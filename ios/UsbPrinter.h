
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNUsbPrinterSpec.h"

@interface UsbPrinter : NSObject <NativeUsbPrinterSpec>
#else
#import <React/RCTBridgeModule.h>

@interface UsbPrinter : NSObject <RCTBridgeModule>
#endif

@end
