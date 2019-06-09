package com.example.android.faciallandmarkdetection;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.faciallandmarkdetection.MvUtils.DetectLandmarks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import static org.opencv.core.CvType.CV_8UC4;

public class CameraActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private static CascadeClassifier classifier;

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;
    Mat mRGBA;
    Mat mGray;
    FaceDetection faceDetector;
    private int absoluteFaceSize;
    private MatOfRect faceDetections = new MatOfRect();
    private Mat grayscaleImage = new Mat();
    private Mat inputFrameProcessed = new Mat();
    private int frameCount= 0;
    private int frameSkipCount=20; //How many frames to skip for facedetection
    private ImageView imageCanvas;
    private boolean process = false;
    private DetectLandmarks landmarkDetector;

    private Rect faceRoi;
    //Bitmaps declarations
    private Bitmap processedImage;
    private Mat processedImageMat;
    private Bitmap croppedFaceImage;

    private int MAX_WIDTH = 800;
    private int MAX_HEIGHT = 800;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);
        mOpenCvCameraView = findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        imageCanvas = findViewById(R.id.image_canvas);
        landmarkDetector = new DetectLandmarks(this);

        mOpenCvCameraView.setMaxFrameSize(MAX_WIDTH,MAX_HEIGHT);

        load_cascade();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height, width, CV_8UC4);
        mGray = new Mat(height, width, CV_8UC4);
        faceDetector = new FaceDetection(this, mRGBA);

        //Bitmap must be initialized like this before using it in the code, otherwise android throws
        //a bmp==null exception
        processedImage = Bitmap.createBitmap(mRGBA.width(),mRGBA.height(),Bitmap.Config.ARGB_8888);
        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);

    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        //return faceDetection(inputFrame.rgba());
        mRGBA =  inputFrame.rgba();
        mGray = inputFrame.gray();
        //faceDetection(mRGBA);
        if(process){

            faceDetection(mRGBA);
            detectLandmarks2(mRGBA);

        }
        //Mat x=new Mat();
       // Utils.bitmapToMat(processedImage,x);
        //return faceDetection(mRGBA);
        return inputFrame.gray();
    }

    /**
     * Uses the FaceDetection Class to perform face detection
     * This method also sets the global variable FaceRoi after performing the
     * face detection steps for further processing e.g. cropping the full
     * image to the face ROI region
     */
    public Mat faceDetection(Mat inputFrame) {

        if(frameCount == 0){
            frameCount++;

            Imgproc.cvtColor(inputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);

            if (classifier.empty()) {
                Log.v(TAG,"Classifier not found");
                return inputFrame;

            }

            else {
                //classifier.detectMultiScale(grayscaleImage, faceDetections);

                classifier.detectMultiScale(grayscaleImage, faceDetections, 1.1, 2, 2,
                        new Size(absoluteFaceSize, absoluteFaceSize), new Size());
                // to draw rectangles around the detected faces
                for (Rect rect : faceDetections.toArray()) {
                    Imgproc.rectangle(
                            inputFrame,                                               // where to draw the box
                            new Point(rect.x, rect.y),                            // bottom left
                            new Point(rect.x + rect.width, rect.y + rect.height), // top right
                            new Scalar(0, 0, 255),                 // RGB colour and thickness of the box
                            1
                    );
                    setFaceRoi(rect);
                    //croppedFaceROI = new Mat(inputFrame,new Rect(rect.x, rect.y,rect.x +
                            //rect.width, rect.y + rect.height));
                }
                return inputFrame;
            }
        }

        else if(frameCount >0 && frameCount <= frameSkipCount){
            frameCount++;

            for (Rect rect : faceDetections.toArray()) {
/*                Imgproc.rectangle(
                        inputFrame,                                               // where to draw the box
                        new Point(rect.x, rect.y),                            // bottom left
                        new Point(rect.x + rect.width, rect.y + rect.height), // top right
                        new Scalar(0, 0, 255),                 // RGB colour and thickness of the box
                        1
                );*/
                setFaceRoi(rect);
            }
            return inputFrame;
        }
        else if(frameCount == frameSkipCount+1 ){
            frameCount = 0;
            for (Rect rect : faceDetections.toArray()) {
/*                Imgproc.rectangle(
                        inputFrame,                                               // where to draw the box
                        new Point(rect.x, rect.y),                            // bottom left
                        new Point(rect.x + rect.width, rect.y + rect.height), // top right
                        new Scalar(0, 0, 255),                 // RGB colour and thickness of the box
                        1
                );*/
                setFaceRoi(rect);
            }
            return inputFrame;
        }
        return inputFrame;
    }




    public void load_cascade(){
        try {
            InputStream is = this.getResources().openRawResource(R.raw.lbpcascade_frontalface_improved);
            File cascadeDir = this.getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            classifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if(classifier.empty())
            {
                Log.v("MyActivity","----(!)Error loading \n");
                return;
            }
            else
            {
                Log.v("MyActivity",
                        "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("MyActivity", "Failed to load cascade. Exception thrown: " + e);
        }
    }

    //****************************Methods for landmark detection process****************************

    public void startProcess(View v){
        //Utils.matToBitmap(mRGBA,processedImage);
        //imageCanvas.setImageBitmap(processedImage);
        if(!process){
            process = true;
        }
        else{
            process = false;
        }

        //Mat cropped = new Mat(uncropped, roi);
    }


    public void detectLandmarks2(Mat inputFrame){
        try {

            Rect faceROI= getFaceRoi();

            faceROI.x = faceROI.x - 50;
            faceROI.y = faceROI.y - 50;
            faceROI.width  = faceROI.width + 100;
            faceROI.height = faceROI.height + 100;

            if(faceROI.x <0 ){
                faceROI.x = 0;
            }
            if(faceROI.y <0 ){
                faceROI.y = 0;
            }
            if(faceROI.width > MAX_WIDTH ){
                faceROI.width = MAX_WIDTH;
            }
            if(faceROI.height <0 ){
                faceROI.height = MAX_HEIGHT;
            }

            //Matrix to hold the information of the cropped image
            Mat cropped = new Mat(inputFrame, faceROI);
            croppedFaceImage = Bitmap.createBitmap(cropped.width(),cropped.height(),Bitmap.Config.ARGB_8888);
            processedImage = Bitmap.createBitmap(inputFrame.width(),inputFrame.height(),Bitmap.Config.ARGB_8888);
            processedImage = Bitmap.createBitmap(cropped.width(),cropped.height(),Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(cropped,croppedFaceImage);
            processedImage = landmarkDetector.detFacesFromBitmap(croppedFaceImage);

            Log.v("Size of inputframe: ", inputFrame.size().toString());
            Log.v("Size of ROI: ", faceROI.toString());
        }
        catch (Exception E){
            Log.v(TAG, E.toString());
        }

        //The portion of the background task that updates the UI has to run on the main thread.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //all the code that updates the UI goes in this block
                imageCanvas.setImageBitmap(processedImage);
                //imageCanvasLips.setImageBitmap(croppedLipsImage);

            }
        });

    }


    public Rect getFaceRoi() {
        return faceRoi;
    }

    public void setFaceRoi(Rect faceRoi) {
        this.faceRoi = faceRoi;
    }
}