package com.example.android.faciallandmarkdetection;


import android.app.Activity;
import android.graphics.Bitmap;
import android.util.TimingLogger;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.android.Utils;
import android.graphics.Bitmap;

public class EdgeDetection {
    private Bitmap imgToProcess;
    private Bitmap processedImg;




    public void setImgToProcess(Bitmap b){
        imgToProcess = b;
    }

    /**
     * Performs Canny edge detection as implemented in OpenCV on the bitmap passed from the
     * MainActivity
     */
    public void detectEdges() {
        TimingLogger timing = new TimingLogger("CannyTiming", "Processing Time");
        Mat imageData = new Mat();
        Utils.bitmapToMat(imgToProcess, imageData);

        Mat edges = new Mat(imageData.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(imageData, edges, Imgproc.COLOR_RGB2GRAY, 4);
        Imgproc.Canny(edges, edges, 80, 100);
        //matToBitmap(edges,processedImg);

        processedImg = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(edges, processedImg);
        timing.addSplit("Edge Detection Done...");
        timing.dumpToLog();

    }

    public Bitmap getProcessedImg(){
        return processedImg;
    }


}
