package com.example.android.faciallandmarkdetection.MvUtils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TimingLogger;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.faciallandmarkdetection.DlibLandmarksActivity;
import com.example.android.faciallandmarkdetection.FaceDetection;
import com.example.android.faciallandmarkdetection.R;
import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import org.opencv.core.MatOfRect;

import java.io.File;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.DOWNLOAD_SERVICE;

public class DetectLandmarks {
    final private static String modelFileName = "shape_predictor_68_face_landmarks.dat";
    final private static String shapePredictorUrl = "https://drive.google.com/uc?authuser=0&id=1IWgPwTyI9BHdAseUqfOHLSuoHpKNMecv&export=download";
    private static String uriString = null;
    private static File shapeModel;
    private static String mShapeModel; //path of shape model in downloads
    private static File shapeModelFileURI;
    private static FaceDet mFaceDet;
    private Uri imgURI;
    private Bitmap bitmap = null;
    private static final String TAG = "dlibTiming";
    private List<VisionDetRet> mFaceList = new ArrayList<>();
    private Resources res;
    private Context mContext;
    private Bitmap imgProcessed;
    private float mResizeRatio = 1;
    public Rect lipsROI;

    private MatOfRect faceDetections = new MatOfRect();

    public DetectLandmarks(Context context, Bitmap b) {
        this.mContext = context;
        this.bitmap = b;
        isShapeModelAvailable();
        if (mFaceDet == null) {
            mFaceDet = new FaceDet(mShapeModel);
        }
    }

    public DetectLandmarks(Context context) {
        this.mContext = context;
        isShapeModelAvailable();
        if (mFaceDet == null) {
            mFaceDet = new FaceDet(mShapeModel);
        }
    }

    /**
     * Face Detection and Facial Landmarks detection, along with performance measurements
     * are done in this method
     */
    public Bitmap detFaces(){

        //Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(), R.raw.sample);
        Bitmap bm = bitmap;
        //*****Detector Initialization with face-model ********//
//        if (mFaceDet == null) {
//            mFaceDet = new FaceDet(mShapeModel);
//        }

        //Log.d("dlibActivity","Test log message");

        //following adb shell command is used to make the TimingLogger dump the messages to log.
        //adb shell setprop log.tag.MyTag VERBOSE

        TimingLogger timing = new TimingLogger("dlibTiming", "Processing Time");
        //timing.reset();
        List<VisionDetRet> faceList = mFaceDet.detect(bm);
        // ... Add split in timing with label Face Detection ...
        timing.addSplit("Face Detection Done");

        bm = bitmapOperations(bm);

        //********************************************************************************
        // Create a canvas to draw face rectangles and landmark circles
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        // ... Add split in timing before starting landmark detection ...
        timing.addSplit("Bitmap manipulation");

        float circleRadius = 4f;  // float value for the circle to be drawn at the landmark

        //get corresponding dp value for the radius
        circleRadius = convertPixelsToDp(circleRadius, mContext);

        //Loop through the detected faces and draw rectangles on canvas
        for (VisionDetRet ret : faceList) {
            Rect bounds = new Rect();
            bounds.left = (int) (ret.getLeft() * mResizeRatio);
            bounds.top = (int) (ret.getTop() * mResizeRatio);
            bounds.right = (int) (ret.getRight() * mResizeRatio);
            bounds.bottom = (int) (ret.getBottom() * mResizeRatio);
            canvas.drawRect(bounds, paint);
            // Detect landmarks on the detected faces
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            for (Point point : landmarks) {
                int pointX = (int) (point.x * mResizeRatio);
                int pointY = (int) (point.y * mResizeRatio);
                canvas.drawCircle(pointX, pointY, circleRadius, paint);

            }
        }
        // ... Add split in timing with label Face Detection ...
        timing.addSplit("Facial landmarks detection");
        timing.dumpToLog();

        Toast.makeText(mContext, "Done!", Toast.LENGTH_SHORT).show();
        return bm;


    }

    public Bitmap detFacesFromBitmap(Bitmap b){

        //Bitmap bm = BitmapFactory.decodeResource(mContext.getResources(), R.raw.sample);
        Bitmap bm = b;
        //*****Detector Initialization with face-model ********//
//        if (mFaceDet == null) {
//            mFaceDet = new FaceDet(mShapeModel);
//        }

        //Log.d("dlibActivity","Test log message");

        //following adb shell command is used to make the TimingLogger dump the messages to log.
        //adb shell setprop log.tag.MyTag VERBOSE

        //TimingLogger timing = new TimingLogger("dlibTiming", "Processing Time");
        //timing.reset();
        List<VisionDetRet> faceList = mFaceDet.detect(bm);
        // ... Add split in timing with label Face Detection ...
        //timing.addSplit("Face Detection Done");

        bm = bitmapOperations(bm);

        //********************************************************************************
        // Create a canvas to draw face rectangles and landmark circles
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(2);
        paint.setTextSize(16);
        paint.setStyle(Paint.Style.STROKE);

        // ... Add split in timing before starting landmark detection ...
        //timing.addSplit("Bitmap manipulation");

        float circleRadius = 4f;  // float value for the circle to be drawn at the landmark

        //get corresponding dp value for the radius
        circleRadius = convertPixelsToDp(circleRadius, mContext);

        //Loop through the detected faces and draw rectangles on canvas#
        int i = 0;
        for (VisionDetRet ret : faceList) {
            Rect bounds = new Rect();
            bounds.left = (int) (ret.getLeft() * mResizeRatio);
            bounds.top = (int) (ret.getTop() * mResizeRatio);
            bounds.right = (int) (ret.getRight() * mResizeRatio);
            bounds.bottom = (int) (ret.getBottom() * mResizeRatio);
            canvas.drawRect(bounds, paint);
            // Detect landmarks on the detected faces
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            for (Point point : landmarks) {
                i=i+1;
                int pointX = (int) (point.x * mResizeRatio);
                int pointY = (int) (point.y * mResizeRatio);
                if(i>=49 && i <= 68){
                    paint.setColor(Color.RED);
                    double x= calculateDistance(landmarks.get(64).x,landmarks.get(64).y,
                            landmarks.get(67).x,landmarks.get(67).y);
                    Log.d("Distance 63,67: ",String.format("%.2f", x));
                }
                if(i == 69){

                    paint.setColor(Color.GREEN);
                }
                canvas.drawCircle(pointX, pointY, circleRadius, paint);
            }


        }

        // ... Add split in timing with label Face Detection ...
        //timing.addSplit("Facial landmarks detection");
        //timing.dumpToLog();

        //Toast.makeText(mContext, "Done!", Toast.LENGTH_SHORT).show();
        return bm;


    }

    /**
     * Make an immutable bitmap image into a mutable one
     * resized the bitmap for further processing
     * @param bm Bitmap to edit
     * @return mutable Bitmap
     */
    private Bitmap bitmapOperations(Bitmap bm){
        //****************************Check this part again*****************************************
        android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none

        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are immutable,
        // so we need to convert it to mutable one
        bm = bm.copy(bitmapConfig, true);
        int width = bm.getWidth();
        int height = bm.getHeight();
        // By ratio scale
        float aspectRatio = bm.getWidth() / (float) bm.getHeight();

        final int MAX_SIZE = 512;
        int newWidth = MAX_SIZE;
        int newHeight = MAX_SIZE;
        float resizeRatio = 1;
        newHeight = Math.round(newWidth / aspectRatio);
        if (bm.getWidth() > MAX_SIZE && bm.getHeight() > MAX_SIZE) {
            bm = getResizedBitmap(bm, newWidth, newHeight);
            resizeRatio = (float) bm.getWidth() / (float) width;
            mResizeRatio = resizeRatio;
        }
        return bm;
    }

    /**
     *  Function to get bounds of ROI containing the face using OpenCV
     *  Not being used, only written for checking performance using this
     *  path of implementation
     **/
    public void getFaceDetections(){
        FaceDetection faceDetector = new FaceDetection(mContext);
        imgProcessed =  faceDetector.detectFaces(bitmap);
        faceDetections = faceDetector.getFaceDetections();  //Matrix of rectangles
        org.opencv.core.Rect[] faceArray = faceDetections.toArray();
        //List<VisionDetRet> faceList;
        int numberOfFaces = 0;
        //List<VisionDetRet> mFaceList = ;
        // (String label, float confidence, int l, int t, int r, int b)
        for (org.opencv.core.Rect rect : faceDetections.toArray()) {
            String label = "face" + (numberOfFaces+1);
            float confidence = 1;
            //The X coordinate of the left side of the result //bottom left
            int left = faceArray[numberOfFaces].x;
            //The Y coordinate of the top of the result // bottom left
            int top = faceArray[numberOfFaces].y + faceArray[numberOfFaces].height;
            //The X coordinate of the right side of the result
            int r = faceArray[numberOfFaces].x + faceArray[numberOfFaces].width;
            //The Y coordinate of the bottom of the result
            int b = faceArray[numberOfFaces].y;

            //VisionDetRet(int left, int top, int right, int bottom, String label, float confidence)
            VisionDetRet face= new VisionDetRet(left,top,r,b,label,confidence);
            //Log.d("Face Object Left: ",Integer.toString(left));
            //Log.d("Face Object state", face.toString());
            mFaceList.add(face);
            numberOfFaces++;
        }

        Log.d("Number of Facezzz", Integer.toString(mFaceList.size()));

    }

    /**
     *
     * @param bm Bitmap to resize
     * @param newWidth the new width to scale the image to
     * @param newHeight the new height to scale the image to
     * @return resized bitmap image
     */
    private Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bm, newWidth, newHeight, true);
        return resizedBitmap;
    }

    /**
     * converts a float type pixel value to the corresponding value in dp also float type
     * @param px Pixels value to be converted
     * @param context
     * @return corresponding dp value float data type
     */
    public static float convertPixelsToDp(float px, Context context){
        return px / ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    private Double calculateDistance(int x1, int y1, int x2, int y2){
        double x =Math.sqrt((x2-x1)^2 + (y2-y1)^2);
        return x;

    }


    // ****************************Download Shape predictor*****************************************

    /**
     * This method first checks if the shape_predictor_68_face_landmarks.dat model is already
     * available in the downloads folder
     * <p>
     * Then checks if it has been downloaded already and a valid uri exists.
     * <p>
     * If not then it proceeds to downloading the shape model from the internet.
     * <p>
     * URL is provided in a constant
     * <p>
     * TODO: Add an xml or a csv file in resources to hold Flags that need persistence.
     */
    private void isShapeModelAvailable() {
        shapeModel = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + modelFileName);

        // Check if Shape model already is available in the Downloads Folder
        if (shapeModel.exists()) {
            //Log.v("DetectLandmarks", res.getString(R.string.toast_shapemodel_found));
            Toast.makeText(mContext, R.string.toast_shapemodel_found,
                    Toast.LENGTH_SHORT).show();

            mShapeModel = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                    + File.separator + modelFileName;
        } else if (shapeModelFileURI != null && shapeModelFileURI.isFile()) {
            //Log.v("DetectLandmarks", res.getString(R.string.toast_shapemodel_found_URI));
            Toast.makeText(mContext, R.string.toast_shapemodel_found_URI,
                    Toast.LENGTH_SHORT).show();
        } else {

            //Log.v("DetectLandmarks", res.getString(R.string.toast_shapemodel_404));
            Toast.makeText(mContext, R.string.toast_shapemodel_404,
                    Toast.LENGTH_SHORT).show();
            downloadShapeModel();
        }


    }

    /**
     * If the shape_predictor_68_face_landmarks.dat is not available already
     * then the model will be downloaded by the application
     * the Uri will be passed from the Async download funciton to this function to create a local
     * file with in the App's folder
     */
    private void setShapeModelURI() {
        if (uriString != null) {

            //Copy the downloaded file to  the app's internal cache directory
            getTempFile(uriString);

            //chk if the file has been created successfully :: for debugging

            if (shapeModelFileURI.isFile()) {
                Log.v("DetectLandmarks", res.getString(R.string.toast_shapemodel_found_URI));
            } else {
                Log.v("DetectLandmarks", res.getString(R.string.toast_shapemodel_404));
            }
        } else {
            Log.v("DetectLandmarks", "uri String is null");
        }

    }

    /**
     * The following method extracts the file name from a URL and creates a file with that name
     * in the app's internal cache directory
     *
     * @param url Uri of the download file in String format
     */
    private void getTempFile(String url) {
        //File file;
        try {
            String fileName = Uri.parse(url).getLastPathSegment();
            shapeModelFileURI = File.createTempFile(fileName, null, mContext.getCacheDir());
        } catch (IOException e) {
            // Error while creating file
        }
        //return file;
    }


    /**
     * Creates an object of type ShapeFileDownloader to download a file(pre-trained template file)
     * from the internet
     */
    private void downloadShapeModel() {
        ShapeFileDownloader fileDownloader = new ShapeFileDownloader();
        fileDownloader.execute();


    }

    /**
     * Class for Async Task to handle downloading files from the internet
     * This handles downloading the shape model from the internet
     * <p>
     * TODO: Add a progress dialog to show the status of download.
     */
    protected class ShapeFileDownloader extends AsyncTask<Void, Void, String> {
        //changed private to protected in class name
        private String url;
        DownloadManager dm;
        private long qID;

        @Override
        protected String doInBackground(Void... params) {
            dm = (DownloadManager) mContext.getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(shapePredictorUrl));

            qID = dm.enqueue(request);
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                        DownloadManager.Query reQuery = new DownloadManager.Query();
                        reQuery.setFilterById(qID);
                        Cursor c = dm.query(reQuery);

                        if (c.moveToFirst()) {
                            int columnIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);

                            if (DownloadManager.STATUS_SUCCESSFUL == c.getInt(columnIndex)) {

                                uriString = c.getString(c.getColumnIndex((DownloadManager.COLUMN_LOCAL_URI)));

                                setShapeModelURI();

                                Log.d("FileDownload", "Successful");
                                Toast.makeText(mContext, R.string.
                                                toast_shapemodel_downloaded,
                                        Toast.LENGTH_SHORT).show();

                            }
                            if (DownloadManager.STATUS_FAILED == c.getInt(columnIndex)) {
                                Log.d("FileDownload", "Failed");
                            }
                        }
                    }
                }
            };

            mContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            return url;
        }

        @Override
        protected void onPostExecute(String s) {
            if (shapeModel.exists()) {
                Toast.makeText(mContext, R.string.toast_shapemodel_downloading,
                        Toast.LENGTH_SHORT).show();
            }
            //super.onPostExecute(s);
        }
    }
}