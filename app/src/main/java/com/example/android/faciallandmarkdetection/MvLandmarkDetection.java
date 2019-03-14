/**
 * Activity class to do the image processing for facial landmark detection using
 * mobile vision API.
 * Bitmap loaded in the main activity is passed to this activity in an Intent
 *
 * TODO: The size of passed bitmap through intents is very limited. For improved performance it should be handled for bigger sizes.
 */


package com.example.android.faciallandmarkdetection;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.util.TimingLogger;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.android.faciallandmarkdetection.MvUtils.FaceView;
import com.example.android.faciallandmarkdetection.MvUtils.SafeFaceDetector;
import com.example.android.faciallandmarkdetection.R;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.IOException;
import java.io.InputStream;

public class MvLandmarkDetection extends AppCompatActivity {
    private static final String TAG = "PhotoViewerActivity";
    private Bitmap bitmap = null;
    private Uri imgURI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mv_landmark_detection);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        //bitmap = intent.getParcelableExtra("BitmapImage");
        imgURI = Uri.parse(message);
        getImageFromURI(imgURI);

        //InputStream stream = getResources().openRawResource(R.raw.face);
        //Bitmap bitmap = BitmapFactory.decodeStream(stream);

        // A new face detector is created for detecting the face and its landmarks.
        //
        // Setting "tracking enabled" to false is recommended for detection with unrelated
        // individual images (as opposed to video or a series of consecutively captured still
        // images).  For detection on unrelated individual images, this will give a more accurate
        // result.  For detection on consecutive images (e.g., live video), tracking gives a more
        // accurate (and faster) result.
        //
        // By default, landmark detection is not enabled since it increases detection time.  We
        // enable it here in order to visualize detected landmarks.
        FaceDetector detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();

        // This is a temporary workaround for a bug in the face detector with respect to operating
        // on very small images.  This will be fixed in a future release.  But in the near term, use
        // of the SafeFaceDetector class will patch the issue.
        Detector<Face> safeDetector = new SafeFaceDetector(detector);

        // Create a frame from the bitmap and run face detection on the frame.
        TimingLogger timing = new TimingLogger("VisionAPITiming", "Processing Time");

        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        SparseArray<Face> faces = safeDetector.detect(frame);

        if (!safeDetector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        timing.addSplit("Face Detection Done...");

        FaceView overlay = findViewById(R.id.faceView);
        overlay.setContent(bitmap, faces);
        overlay.bringToFront();


        int numberFaces = 0;
        for (int i = 0; i < faces.size(); ++i) {
            Face face = faces.valueAt(i);
            for (Landmark landmark : face.getLandmarks()) {
                ++numberFaces;
            }
        }
        timing.addSplit("Landmark detection Done...");
        String nF = Integer.toString(numberFaces);
        String tooost = nF+ "/8 landmarks detected ";
        Toast.makeText(MvLandmarkDetection.this, tooost, Toast.LENGTH_SHORT).show();
        timing.dumpToLog();

        // Although detector may be used multiple times for different images, it should be released
        // when it is no longer needed in order to free native resources.
        safeDetector.release();

    }

    /**
     * This method loads the bitmap from device memory by parsing the URI
     * @param imgURI URI of the image file to be processed
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

}
