package com.example.android.faciallandmarkdetection;

import android.app.DownloadManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TimingLogger;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/*import com.example.android.faciallandmarkdetection.dlibAccess.Constants;
import com.example.android.faciallandmarkdetection.dlibAccess.FaceDet;
import com.example.android.faciallandmarkdetection.dlibAccess.VisionDetRet;*/

import com.tzutalin.dlib.FaceDet;
import com.tzutalin.dlib.VisionDetRet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DlibLandmarksActivity extends AppCompatActivity{
    final private static String modelFileName = "shape_predictor_68_face_landmarks.dat";
    final private static String shapePredictorUrl ="https://drive.google.com/uc?authuser=0&id=1IWgPwTyI9BHdAseUqfOHLSuoHpKNMecv&export=download";
    private static String uriString= null;
    private static File shapeModel;
    private static String mShapeModel; //path of shape model in downloads
    private static File shapeModelFileURI;
    private FaceDet mFaceDet;
    private Uri imgURI;
    private Bitmap bitmap = null;
    private static final String TAG = "dlibTiming";


    private List<VisionDetRet> faceList;
    //private FaceDet mFaceDet;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dlib_landmarks);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        TextView userMessages = findViewById(R.id.user_prompts);
        userMessages.setText(message);
        imgURI = Uri.parse(message);
        getImageFromURI(imgURI);
        isShapeModelAvailable();


    }

    /** Image is loaded in the MainActivity and then the URI of that image is passed
     * between the activities. The URI is then parsed and the corresponding image from the
     * device memory is loaded again into the application by this method.
     *
     * @param imgURI URI of the image to be processed.
     */

    protected void getImageFromURI(Uri imgURI){

        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgURI);

            //*******for debugging******
            //ImageView iv = findViewById(R.id.image_canvas);
            //iv.setImageBitmap(bitmap);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
/*    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            imgURI = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imgURI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }*/

    // ********************Face Detection and Landmarks using dlib**********************************

    /**
     * Face Detection and Facial Landmarks detection, along with performance measurements
     * are done in this method
     */
    private void detFaces(){
        ImageView iv = findViewById(R.id.image_canvas);
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.raw.sample);

        if (bitmap==null) {
            //for debugging, loads sample.bmp from the raw folders
            iv.setImageBitmap(bm);
        }
        else{
            bm=bitmap;
            iv.setImageBitmap(bitmap);
        }

        //*****Detector Initialization with face-model ********//
        if (mFaceDet == null) {
            mFaceDet = new FaceDet(mShapeModel);
        }

        //Log.d("dlibActivity","Test log message");

        //following adb shell command is used to make the TimingLogger dump the messages to log.
        //adb shell setprop log.tag.MyTag VERBOSE

        TimingLogger timing = new TimingLogger("dlibTiming", "Processing Time");
        //timing.reset();
        List<VisionDetRet> faceList = mFaceDet.detect(bm);
        // ... Add split in timing with label Face Detection ...
        timing.addSplit("Face Detection Done");

        //****************************Check this part again*****************************************
       android.graphics.Bitmap.Config bitmapConfig = bm.getConfig();
        // set default bitmap config if none
        if (bitmapConfig == null) {
            bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
        }
        // resource bitmaps are imutable,
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
        }



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
        circleRadius = convertPixelsToDp(circleRadius, this);

        //Loop through the detected faces and draw rectangles on canvas
        for (VisionDetRet ret : faceList) {
            Rect bounds = new Rect();
            bounds.left = (int) (ret.getLeft() * resizeRatio);
            bounds.top = (int) (ret.getTop() * resizeRatio);
            bounds.right = (int) (ret.getRight() * resizeRatio);
            bounds.bottom = (int) (ret.getBottom() * resizeRatio);
            //canvas.drawRect(bounds, paint);
            // Detect landmarks on the detected faces
            ArrayList<Point> landmarks = ret.getFaceLandmarks();
            for (Point point : landmarks) {
                int pointX = (int) (point.x * resizeRatio);
                int pointY = (int) (point.y * resizeRatio);
                canvas.drawCircle(pointX, pointY, circleRadius, paint);

            }
        }
        // ... Add split in timing with label Face Detection ...
        timing.addSplit("Facial landmarks detection");
        timing.dumpToLog();

        //set the ImageView to the canvas with the drawn information
        iv.setImageDrawable(new BitmapDrawable(getResources(), bm));
        Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show();

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

    // ****************************Download Shape predictor*****************************************
    /**
     * This method first checks if the shape_predictor_68_face_landmarks.dat model is already
     * available in the downloads folder
     *
     * Then checks if it has been downloaded already and a valid uri exists.
     *
     * If not then it proceeds to downloading the shape model from the internet.
     *
     * URL is provided in a constant
     *
     * TODO: Add an xml or a csv file in resources to hold Flags that need persistence.
     */
    private void isShapeModelAvailable(){

        shapeModel = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                + File.separator + modelFileName);

        // Check if Shape model already is available in the Downloads Folder
        if(shapeModel.exists()){
            Toast.makeText(DlibLandmarksActivity.this, R.string.toast_shapemodel_found,
                    Toast.LENGTH_SHORT).show();

            mShapeModel = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()
                    + File.separator + modelFileName;

            Toast.makeText(DlibLandmarksActivity.this, mShapeModel,
                    Toast.LENGTH_SHORT).show();
            //shapeModel.getAbsolutePath();

            detFaces();
        }

        else if (shapeModelFileURI!=null && shapeModelFileURI.isFile()) {
            Toast.makeText(DlibLandmarksActivity.this, R.string.toast_shapemodel_found_URI,
                    Toast.LENGTH_SHORT).show();
            detFaces();
        }
        else{

            Toast.makeText(DlibLandmarksActivity.this, R.string.toast_shapemodel_404,
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
    private void setShapeModelURI(){
        if (uriString != null) {

            //Copy the downloaded file to  the app's internal cache directory
            getTempFile(DlibLandmarksActivity.this, uriString);

            //chk if the file has been created successfully :: for debugging

            if(shapeModelFileURI.isFile()){
                Toast.makeText(DlibLandmarksActivity.this, R.string.toast_shapemodel_found_URI,
                        Toast.LENGTH_SHORT).show();
            }
            else{
                Toast.makeText(DlibLandmarksActivity.this, R.string.toast_shapemodel_404,
                        Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Toast.makeText(DlibLandmarksActivity.this, "uri String is null",
                    Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * The following method extracts the file name from a URL and creates a file with that name
     * in the app's internal cache directory
     *
     * @param context Current Activity
     * @param url Uri of the download file in String format
     */
    private void getTempFile(Context context, String url) {
        //File file;
        try {
            String fileName = Uri.parse(url).getLastPathSegment();
            shapeModelFileURI = File.createTempFile(fileName, null, context.getCacheDir());
        } catch (IOException e) {
            // Error while creating file
        }
        //return file;
    }


    /**
     * Creates an object of type ShapeFileDownloader to download a file(pre-trained template file)
     * from the internet
     */
    private void downloadShapeModel(){
        ShapeFileDownloader fileDownloader =new ShapeFileDownloader();
        fileDownloader.execute();


    }

    /**
     * Class for Async Task to handle downloading files from the internet
     * This handles downloading the shape model from the internet
     *
     * TODO: Add a progress dialog to show the status of download.
     */
    protected class ShapeFileDownloader extends AsyncTask<Void,Void,String>{
        //changed private to protected in class name
        private  String url;
        DownloadManager dm;
        private long qID;
        @Override
        protected String doInBackground(Void... params){
            dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
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
                                Toast.makeText(DlibLandmarksActivity.this, R.string.
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

            registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            return url;
        }

        @Override
        protected void onPostExecute(String s) {
            if (shapeModel.exists()) {
                Toast.makeText(DlibLandmarksActivity.this, R.string.toast_shapemodel_downloading,
                        Toast.LENGTH_SHORT).show();
            }
            //super.onPostExecute(s);
        }
    }
}


