# AVATAR
## INTRODUCTION
###### Project AVATAR - Android application for image processing and Facial Landmarks detection.
###### The project is in fulfilment for Research Seminar(S)[6-ECTs] course in the Master in Embedded Systems for Mechatronics stream @ FH-Dortmund.

The Android application takes the frames form the on-board camera of an android device and uses OpenCV and dlib to extract faces
and lips' region of interest (ROI) from the images. The shared libraries for dlib that I use here are from tzutalin 
```
_(https://github.com/tzutalin/dlib-android)_
```
The input frames with 68 face landmarks drawn on the input image is then returned and shown on screen.

## Future Work
###### Will be implemented for the Research-Project(Thesis) [MOD3-03 // 18-ECTs] Course.
The next goal for this application is to enable lip reading using MachineLearning.
The approach that I am going to use to solve this problem is to use Tensor Flow and train it on some pre-decided 
phrases. Then to deploy the ML part of this software for doing predictions on a PC(a raspberry-pi based server to perform
remote predictions or a PC using client-server architecture).

To implement this ROS will be used to have a communication infrastructure. The android application will publish the frames from the camera with landmarks on a topic. The module on the PC will subscribe to this topic to extract the frames.

The LipsROI will be extracted on the client(android device) transfered to the server(PC/rp) using ROS nodes or passing JSON 
using HTTP/.js in case of rp-server.

The repo named [AVATAR-ROS](https://github.com/ahmadhmirza/Avatar-ROS) contains the implementation for this second phase of the project where ROS is used for client server communication between the android device and PC running on the same network.
