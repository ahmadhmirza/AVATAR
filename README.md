# AVATAR
Project AVATAR - Android application for image processing and Facial Landmarks detection.
The project is in fulfilment for Research Thesis course in the Master in Embedded Systems for Mechatronics stream @ FH-Dortmund.

The Android application takes the frames form the on-board camera of an android device and uses OpenCV and dlib to extracts faces
and lips ROI from the images. The shared libs for dlib that I use here are from tzutalin (https://github.com/tzutalin/dlib-android)

The next goal for this application is to enable lip reading using MachineLearning.
The approach that I am looking at at the moment to solve this problem is to use Tensor Flow and train it on some pre-decided 
phrases. Then to deploy the ML part of this software for doing predictions on a remote PC(a raspberry-pi based server to perform
remote predictions or a PC using client-server architecture, for this approach ROS would be a good choice).

The LipsROI will then be extracted on the client(android device) transfered to the server(PC/rp) using ROS nodes or passing JSON 
using HTTP/.js in case of rp-server.

The repo named AVATAR-ROS contains the implementation for the second scenario where ROS is used for client server communication
between the android device and PC running on the same network.
