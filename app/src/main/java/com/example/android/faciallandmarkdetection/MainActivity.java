/**
 * Project Avatar
 * Facial Landmark Detection Android Application
 * Author: Ahmad Hassan Mirza
 * ahmadhasanmirza@gmail.com
 *
 * Version Summary:
 *
 *  Ver 2.6
 *      Handling for passing the downloaded landmarks model and the bitmap image from java layer to C++ layer.
 *      Facial and Landmark detection using dlib
 *      Select image from gallery for processing.
 *      Implementation of TimingLogger to DlibLandmarksActivity for performance measurements.
 *      UI updates
 *      TODO : Add more checks so the shape model file is not downloaded repeatedly.
 *      TODO : Add dialogue for "processing image" time.
 *      TODO : Update JavaDoc, and arrange code.
 *      TODO : Normalize lip distance with the eye distance
 *
 *  Ver 3.0
 *
 *      TODO: DCT - implementation : Lip ROI - (resize ? ... 32 * 64 image size for lip ROI)
 *      TODO: Transfer to PC
 *          TODO: ~ 100 entries from the DCT matrix to PC - ROS implementation ?
 *          TODO: Hidden Markov Chains/Model
 *          TODO: ~ 3 - 4 Words/ Numbers
 */

package com.example.android.faciallandmarkdetection;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.faciallandmarkdetection.MvUtils.DetectLandmarks;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String IMAGE_DIRECTORY = "/AVATAR";
    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE"; // for intent
    //ImageView imageView;

    private int GALLERY = 1, CAMERA = 2;
    Bitmap imgToProcess; //global bitmap to hold the loaded image
    Bitmap imgProcessed; //Bitmap to hold Edge detected image with Canny Algorithm
    String path; // to hold image's path in directory
    String pathProcessed;  // to hold the path of the processed image
    Bitmap bitmap; // to hold the original image temporarily
    Spinner spinner; // Dropdown menu for operation selection
    private Uri imgURI;
    private TextView debugMsgs;


    public static boolean onCanvas=false; // To hold the status of the image on canvas
    public static boolean imgLoaded=false; // flag to determine if an image is available

    private static final String TAG = "FLD Main Activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FaceDetection faceDetector = new FaceDetection(this);  // for context

        debugMsgs = findViewById(R.id.debug_messages);

    }

    //*****OpenCV Initialization ********//
    static {
        if (OpenCVLoader.initDebug()) {
            Log.i(TAG, "OpenCV initialized successfully");
        } else {
            Log.i(TAG, "OpenCV failed to initialize");
        }
    }
    //***************************************************************************************



    public void loadImage(View v) {
        int requestCode = GALLERY;
        choosePhotoFromGallery();
    }

    public void onClickRotate(View v) {
        if (imgLoaded==true){
            if(onCanvas)
            {
                imgProcessed = rotateImage(imgProcessed);
                displayImage(imgProcessed);
            }
            else
            {
                bitmap = rotateImage(bitmap);
                displayImage(bitmap);
            }
        }
        else{
            showLoadImageToast();
        }

    }

    public void onClickProcessImage(View v){
        if (imgLoaded) {
            //startDlibActivity();
            DetectLandmarks landmarkDetector = new DetectLandmarks(this, imgToProcess);

            imgProcessed = landmarkDetector.detFaces();

            //imgProcessed = landmarkDetector.detectFaceROI();
            displayImage(imgProcessed);
            toggleCanvasStatus();
        }
        else{
            showLoadImageToast();
        }
    }

    public void onClickSwitchImage(View v){
        if (imgLoaded) {
            switchImage();
        }
        else{
            showLoadImageToast();
        }
    }

    public void onClickSaveImageToDir(View v) {
        if (imgLoaded) {
            if (onCanvas) {
                pathProcessed = saveImage(imgProcessed);
            } else {
                path = saveImage(bitmap);
            }

            Toast.makeText(MainActivity.this, R.string.toast_img_saved, Toast.LENGTH_SHORT).show();
        }
        else{
            showLoadImageToast();
        }
    }

    private void showLoadImageToast(){
        Toast.makeText(MainActivity.this, R.string.toast_load_img, Toast.LENGTH_SHORT).show();
    }

/*    private void showPictureDialog(){
        AlertDialog.Builder pictureDialog = new AlertDialog.Builder(this);
        pictureDialog.setTitle("Select Action");
        String[] pictureDialogItems = {
                "Select photo from gallery",
                "Capture photo from camera" };
        pictureDialog.setItems(pictureDialogItems,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                choosePhotoFromGallery();
                                break;
                            case 1:
                                takePhotoFromCamera();
                                break;
                        }
                    }
                });
        pictureDialog.show();
    }*/

    /**
     * Creates an Intent to open the Gallery so an image can be loaded from the device memory
     * for further processing
     */
    public void choosePhotoFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(galleryIntent, GALLERY);
    }

    /**
     * Creates an Intent to open the camera so a new image can be taken and loaded
     * for further processing
     */

    private void takePhotoFromCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA);
    }

    /**
     * Loads an image from the gallery or the camera depending on user preference
     * Displays the image in an ImageView
     * @param requestCode Intent value
     * @param resultCode  Integer value associated with the user's choice
     *                    Defined as constants GALLERY =1 CAMERA=2
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        //imageView = findViewById(R.id.imageview);

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                    //path = saveImage(bitmap);
                    //ExifInterface exif = new ExifInterface(path);
                    //int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                    //rotateImage(bitmap);
                    imgLoaded=true;
                    imgToProcess = bitmap;
                    imgProcessed = bitmap;
                    displayImage(bitmap);
                    imgURI = contentURI;
                    //imageView.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, R.string.toast_process_failed, Toast.LENGTH_SHORT).show();
                }
            }

        }

/*//          CAMERA FUNCTIONALITY NOT IMPLEMENTED YET

            else if (requestCode == CAMERA) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            int angle = 90;

            Matrix matrix = new Matrix();
            imageView.setScaleType(ImageView.ScaleType.MATRIX);   //required
            matrix.setRotate((float) angle);
            imageView.setImageMatrix(matrix);

            imgToProcess = Bitmap.createBitmap(thumbnail,0,0,thumbnail.getWidth(),thumbnail.getHeight(),matrix,true);
            //imageView.setImageBitmap(thumbnail);
            saveImage(thumbnail);
            displayImage(imgToProcess);
            Toast.makeText(MainActivity.this, "Image Saved!", Toast.LENGTH_SHORT).show();
        }*/
    }

    /**
     * Saves the image to the device memory
     * Provides functionality to save the results of image processing
     * @param myBitmap variable of type bitmap holding the image to be saved
     * @return path of the saved image
     */
    public String saveImage(Bitmap myBitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        myBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes);
        File saveDirectory = new File(
                Environment.getExternalStorageDirectory() + IMAGE_DIRECTORY);
        // have the object build the directory structure, if needed.
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }

        try {
            File f = new File(saveDirectory, Calendar.getInstance()
                    .getTimeInMillis() + ".jpg");
            f.createNewFile();
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(bytes.toByteArray());
            MediaScannerConnection.scanFile(this,
                    new String[]{f.getPath()},
                    new String[]{"image/jpeg"}, null);
            fo.close();
            //Log.d("TAG", "File Saved::--->" + f.getAbsolutePath());

            return f.getAbsolutePath();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return "";
    }

    /**
     * displays the image on the ImageView
     * @param b bitmap image to be displayed
     */
    public void displayImage(Bitmap b){
        ImageView imageCanvas = findViewById(R.id.image_canvas);
        imageCanvas.setImageBitmap(b);
    }

    /**
     * Sets the states of which image is being displayed on the ImageView at the moment
     * status is used when saveImage() function is called
     * onCanvas = true means that the processed image is being displayed and vice versa
     */
    public void toggleCanvasStatus(){
        if (onCanvas){
            onCanvas=false;
        }
        else{
            onCanvas=true;
        }
    }

    /**
     * Method to switch the image being displayed between the original and the processed image
     */
    public void switchImage(){
        if (onCanvas){
            //displayImage(imgToProcess);
            displayImage(bitmap);
            toggleCanvasStatus();
        }
        else{
            displayImage(imgProcessed);
            toggleCanvasStatus();
        }

    }


    private static int rotationCounter=0;

    /**
     * This method rotates the image 90 degrees counter clockwise
     * Only rotates the view. Has no permanent effect on the image when it is saved.
     * @param b bitmap of the image to be rotated
     */
    public Bitmap rotateImage(Bitmap b){
        Matrix matrix = new Matrix();
        if (rotationCounter==0){
            matrix.postRotate(90);
            rotationCounter=1;
        }
        else if (rotationCounter==1){
            matrix.postRotate(180);
            rotationCounter=2;
        }
        else if (rotationCounter==2){
            matrix.postRotate(270);
            rotationCounter=3;
        }
        else if (rotationCounter==3){
            matrix.postRotate(360);
            rotationCounter=0;
        }

        b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
        //imageView.setImageBitmap(b);
        return(b);
        //displayImage(b);

    }
    /**
     * Uses the FaceDetection Class to perform face detection
     */
    public void faceDetection(){
        FaceDetection faceDetector = new FaceDetection(this);
        imgProcessed = faceDetector.detectFaces(bitmap);

        //Toast.makeText(MainActivity.this, String.format("Detected %s faces",faceDetector.
                //faceDetections.toArray().length), Toast.LENGTH_SHORT).show();

        String toast = getString(R.string.toast_facedetection_done,faceDetector.
                faceDetections.toArray().length);

        Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();

        displayImage(imgProcessed);
        toggleCanvasStatus();
    }

    /**
     * Uses the DlibLandmarksActivity class to perform facial landmarks detection using Dlib
     * and JNI
     */
    private void startDlibActivity(){
        //Resources res = getResources();
        Uri tempURI = imgURI;
        Intent intent = new Intent(this, DlibLandmarksActivity.class);
        String message = tempURI.toString();
        intent.putExtra(EXTRA_MESSAGE,message);
        startActivity(intent);

    }

    public void onClickTestFlight(View v){
        Intent intent = new Intent(MainActivity.this, CameraActivity.class);
        MainActivity.this.startActivity(intent);
    }
}


