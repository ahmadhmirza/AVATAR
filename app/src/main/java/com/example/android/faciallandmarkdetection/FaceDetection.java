package com.example.android.faciallandmarkdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.util.TimingLogger;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Class for detecting faces and defining a rectangular area around them for further processing
 * Utilizes OpenCV's face detection features#
 *
 * TODO : Incorporate error handling (current implementation line 51)
 */
public class FaceDetection{

    public Mat src = new Mat();   // Matrix to hold the source image data
    public Bitmap processedImg; // to hold the processed image
    public MatOfRect faceDetections = new MatOfRect();
    CascadeClassifier classifier;


    private Context context;

    public FaceDetection(Context current){
        this.context = current;
    }

    /**
     *
     * @param b of type Bitmap, Image to be processed
     * @return Bitmap image with overlay of a bounding rectangle on the detected faces.
     */
    public Bitmap detectFaces(Bitmap b){
        TimingLogger timing = new TimingLogger("OpenCVTiming", "Processing Time");
        processedImg = b;
        Utils.bitmapToMat(b, src); //change bitmap to matrix for processing
        load_cascade(); // function to load the cascade classifier.

        //To handle the cascade classifier being empty
        //Currently returning a grayscale image instead of an error

        if (classifier.empty())
        {
            Mat tmp = new Mat (b.getWidth(), b.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(b, tmp);
            Imgproc.cvtColor(tmp, tmp, Imgproc.COLOR_RGB2GRAY);
            Utils.matToBitmap(tmp, b);
            timing.addSplit("No classifier detection...");
            return b;
        }
        else{
            classifier.detectMultiScale(src, faceDetections);
            // to draw rectangles around the detected faces
            for (Rect rect : faceDetections.toArray()) {
                Imgproc.rectangle(
                        src,                                               // where to draw the box
                        new Point(rect.x, rect.y),                            // bottom left
                        new Point(rect.x + rect.width, rect.y + rect.height), // top right
                        new Scalar(0, 0, 255),                 // RGB colour and thickness of the box
                        4
                );
                timing.addSplit("Face Detection Done...");
                timing.dumpToLog();


            }

            processedImg = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(src, processedImg);

            return processedImg;
        }

    }

    /**
     * Function to load the cascade classifier with the xml file containing the data from
     * training the algorithm to detect faces. The XML is named lbpcascade_frontalface.xml and is
     * included in the OpenCV sdk, for the application it is placed in res/raw directory.
     * This function also has a handle to check if cascade classifier is empty making the one in
     * the detectFaces() redundant.
     **/
    public void load_cascade(){
        try {
            InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface_improved);
            File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
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
}
