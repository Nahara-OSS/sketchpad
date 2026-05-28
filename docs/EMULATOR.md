# Nahara's Sketchpad on device emulators

If you wish to draw on computer with Nahara's Sketchpad, there are some options available.

## Android Emulator

The recommended way of running Android Emulator is to use command-line interface tool. The emulator
embedded directly in Android Studio have some severe performance issues, even though it is using
hardware acceleration. It also doesn't handle stylus input, so it is almost useless for drawing.

- Pass `-gpu host` to use hardware acceleration.

Normally you can use stylus to draw directly in emulator, and sensors like pressure or tilt would
work normally. However, It is highly recommended to set up [USB passthrough][ae-usb-passthrough] to
pass the inputs from graphics tablet directly to emulated device. Doing so will allow Android to
obtain high resolution position data from the tablet.

- On Linux, you would use `lsusb` to find your tablet's vendor and product IDs, then use
  `-usb-passthrough vendorid=VID,productid=PID` as emulator's CLI option, like this:

  ```console
  [user@localhost ~]$ lsusb
  Bus 001 Device 001: ID 1d6b:0002 Linux Foundation 2.0 root hub
  Bus 002 Device 001: ID 1d6b:0003 Linux Foundation 3.0 root hub
  Bus 003 Device 001: ID 1d6b:0002 Linux Foundation 2.0 root hub
  Bus 003 Device 002: ID 27c6:639c Shenzhen Goodix Technology Co.,Ltd. Goodix USB2.0 MISC
  Bus 003 Device 003: ID 0c45:6a21 Microdia Integrated_Webcam_FHD
  Bus 003 Device 004: ID 8087:0033 Intel Corp. AX211 Bluetooth
  Bus 003 Device 013: ID 056a:0357 Wacom Co., Ltd PTH-660 [Intuos Pro (M)]
  Bus 004 Device 001: ID 1d6b:0003 Linux Foundation 3.0 root hub
  [user@localhost ~]$ ./Android/Sdk/emulator/emulator -avd Medium_Tablet -gpu host -usb-passthrough vendorid=0x056a,productid=0x0357
  ```

  In this case, the Intuos Pro M (`056a:0357`) is being passed into emulated device.

## Waydroid (Linux + Wayland only)

Another way to draw on computer is to use Waydroid. The user interface performs much better than
Android Emulator, but it suffers from loss of precision for position data, which could introduces
high amount of wobbles. You can mitigate this by adding a bit of smoothing in app settings.

[ae-usb-passthrough]: https://source.android.com/docs/automotive/start/passthrough