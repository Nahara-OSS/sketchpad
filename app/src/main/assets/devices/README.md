# Device-specific configurations

This folder provides the configurations, such as default optimal settings and calibration data for
specific devices. The folder structure must is as follows:

- `devices/`
    - Manufacturer ID (`Build.MANUFACTURER`)
        - Device model ID (`Build.DEVICE`)
            - `settings.json`
            - `features.json`

## `settings.json`

This is the **settings overlay** file that sits on top of default settings. This is for providing
default optimal configurations for each device.

## `features.json`

This configuration enable or disable some features. Mainly for emulated devices.