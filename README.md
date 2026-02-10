# Nahara's Sketchpad

Companion sketching app for, well, sketching!.

Nahara's Sketchpad is not an all-in-one drawing app - the main purpose is to offer a sketching pad
for you to draw on, then later transfer your sketches to fully featured drawing app, like Krita for
example. That means Sketchpad does not have powerful brush engine, filters, advanced editing tools
or anything you'd typically see in professional drawing apps.

This app provides the following features:

- **Low latency canvas**
- **Infinite canvas**
- **Basic layer system**: Renaming, reordering, changing blending mode and opacity.
- **Stamp-based brush engine**: Basically it stamps the brush multiple times.
- **Strip-based brush engine**: It's like sticking textured tape to the canvas.
- **Quick open in Krita**: Not only does it open your sketch in Krita, but it also preserves your
  layer stack!

## Compatibility

In general, if your Android device is running Android 12 and supports OpenGL ES 3.0 or higher, and
OpenGL ES implementation is correctly implemented, that device should be able to use Nahara's
Sketchpad. However, there are some cases where graphical glitches may appears, especially when
enabling low latency mode. Below is the table of all previously tested devices and whether it is
working properly.

| Model number | Device                     | Does it works? | Note                                       |
|--------------|----------------------------|----------------|--------------------------------------------|
| SM-N975F     | Samsung Galaxy Note 10+    | Partially      | Low latency mode may introduces flickering |
| (emulator)   | Waydroid 1.6.1             | **No**         | Canvas is fully black                      |
| (emulator)   | Android Emulator 36.3.10.0 | **No**         | Canvas is fully black                      |

When encountering issues, please consider file a new issue in [Issues][issues] tab. Make sure to
include model number of your device. If your device is an emulator, please include the name of
emulator along with version number.

## License

Nahara's Sketchpad is licensed under [MIT License][license].

[issues]: http://about:blank

[license]: ./LICENSE
