# Focus-Cam
Camera that uses the Acceleration Sensor to trigger Auto Focus

![Alt text](/screenshot.jpg)

##Camera Hardware

On Devices like the Nexus 7 2012 which only have an Front Facing Camera a naive call to Camera#open
will return null. Code checks before it call the Camera#open with an index to query the 'best' camera
available.

##Focus

The APP does not take photos or record videos, it simply shows an live preview which may be in wrong orientation
when displayed on devices. I did not take much care about the preview itself as it intention was to get the
focusing working well.

## Acceleration
The focusing is triggered by the Sensor Data, it also tries not to stress the device to much by constantly
requesting to focus.
