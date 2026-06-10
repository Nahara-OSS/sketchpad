# Nahara's Sketchpad on device emulators

If you wish to draw on computer with Nahara's Sketchpad, there are some options
available:

- [Android Desktop](#android-desktopchromeos)
- [Android Emulator](#android-emulator-standalone)
- [Waydroid](#waydroid-linux--wayland-only)

## Android Desktop/ChromeOS

The upcoming Googlebook running Android 17 (and the older Chromebook with
ChromeOS) might be able to run Nahara's Sketchpad, though the user interface is
not optimized for Android Desktop. We are still considering the UI design that
adapts well to desktop screens.

## Android Emulator (standalone)

Nahara's Sketchpad is continously tested with Android Emulator, and so Android
Emulator is supported.

The recommended way of running Android Emulator is to use command-line interface
tool. The emulator embedded directly in Android Studio have some severe
performance issues, even though it is using hardware acceleration. The embedded
emulator also doesn't handle stylus input, so it is almost useless for drawing.

### Setting up AVD

Android Virtual Device (AVD) can be created and configured inside Android
Studio. Once created, the AVD can be launched using standalone emulator. It is
recommended to use Android 14 or newer, as Android 14 introduces improved
graphics tablet support. To launch the AVD, pass the `-avd` flag:

```console
$ emulator -avd Resizable_Experimental
```

Note that the name of AVD only contains `[A-Za-z0-9_]` characters. For example,
if the display name of AVD inside Android Studio is `Resizable (Experimental)`,
the flag to pass to CLI is `-avd Resizable_Experimental`.

Also please note that the base AOSP image might not provide tilt direction
data. You might need the system image with Google Play Service so that the app
can receive tilt direction data through `MotionEvent` (or any system image that
fill this missing data probably).

### GPU acceleration

Nahara's Sketchpad uses OpenGL ES 3.0 for canvas rendering and brush engines,
and while Android Emulator can provide software implementation of OpenGL ES, it
is highly recommended to use the host GPU by passing `-gpu host` CLI flag.

```console
$ emulator -avd Resizable_Experimental -gpu host
```

### USB passthrough

Although the emulator can detect tablet inputs from system pointer, it suffers
from precision loss on position data (most likely due to the emulator window
rounding the value of system pointer's position). If you are using USB graphics
tablet and Android 14 inside emulator, you can use
[USB passthrough][ae-usb-passthrough] to pass graphics tablet hardware input
directly to Android by using `-usb-passthrough vendorid=VID,product=PID` flag
(VID and PID must have `0x` prefix when passing the flag).

On Linux, vendor ID (VID) and product ID (PID) can be found by using `lsusb`:

```console
$ lsusb
Bus 001 Device 001: ID 1d6b:0002 Linux Foundation 2.0 root hub
Bus 002 Device 001: ID 1d6b:0003 Linux Foundation 3.0 root hub
Bus 003 Device 001: ID 1d6b:0002 Linux Foundation 2.0 root hub
Bus 003 Device 002: ID 27c6:639c Shenzhen Goodix Technology Co.,Ltd. Goodix USB2.0 MISC
Bus 003 Device 003: ID 0c45:6a21 Microdia Integrated_Webcam_FHD
Bus 003 Device 004: ID 8087:0033 Intel Corp. AX211 Bluetooth
Bus 003 Device 013: ID 056a:0357 Wacom Co., Ltd PTH-660 [Intuos Pro (M)]
Bus 004 Device 001: ID 1d6b:0003 Linux Foundation 3.0 root hub
```

In the example shell command output above, the PID:VID of graphics tablet (Wacom
Intuos Pro M) is `056a:0357`, so the flag to enable USB passthrough is
`-usb-passthrough vendorid=0x056a,productid=0x0357`.

```console
$ emulator -avd Resizable_Experimental -gpu host -usb-passthrough vendorid=0x056a,productid=0x0357
```

## Waydroid (Linux + Wayland only)

Nahara's Sketchpad works in Waydroid since the app targets Android 12 as minimum
version.

Waydroid may be more preferable to some users as the user interface is more
fluid (Waydroid is a container after all), but we still don't recommend Waydroid
due to loss of input precision for stylus position data, which could introduce
noticable amount of wobble and jitter. You can mitigate this by adding a bit of
smoothing in app settings.

[ae-usb-passthrough]: https://source.android.com/docs/automotive/start/passthrough
