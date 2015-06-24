package com.shinchven.simplecamera.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "camera_activity";
    private Camera mCamera;
    private FrameLayout mCameraPreviewContainer;
    private CameraPreview mCameraPreview;
    private DisplayUtil.DisplayMatrix mScreenDisplayMatrix;
    private Camera.Size mOptimalPreviewSize;
    private MediaRecorder mMediaRecorder;
    private Button mRecordBtn;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (!checkCameraHardware(this)) {
            Toast.makeText(this, "your camera is not ready..", Toast.LENGTH_SHORT);
            finish();
            return;
        }
        mRecordBtn = ((Button) findViewById(R.id.record));
        mRecordBtn.setOnClickListener(this);
        mScreenDisplayMatrix = DisplayUtil.getScreenDisplayMatrix(this);
        mCameraPreviewContainer = ((FrameLayout) findViewById(R.id.camera_preview_container));
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
        releaseMediaRecorder();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mCamera == null) {
            mCamera = getCameraInstance();


            ViewGroup.LayoutParams layoutParams = mCameraPreviewContainer.getLayoutParams();
            //Camera.Size pictureSize = mCamera.getParameters().getPictureSize();
            Camera.Size previewSize = mCamera.getParameters().getPreviewSize();
            LogUtil.i("previewSize", previewSize.width + " " + previewSize.height);
            DisplayUtil.DisplayMatrix displaySize = DisplayUtil.zoomWithWidth(
                    mScreenDisplayMatrix.width, previewSize.height, previewSize.width);

            LogUtil.i("displaySize", displaySize.width + " " + displaySize.height);

            layoutParams.height = displaySize.height;
            layoutParams.width = displaySize.width;
            mCameraPreviewContainer.setLayoutParams(layoutParams);


            mCameraPreview = new CameraPreview(this, mCamera);
            mCameraPreviewContainer.removeAllViews();
            mCameraPreviewContainer.addView(mCameraPreview);

        }

    }


    private boolean prepareVideoRecorderAndDisplay() {
        if (mCamera != null) {
            mMediaRecorder = new MediaRecorder();

            // Step 1: Unlock and set camera to MediaRecorder
            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);


            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoSize(640,480);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

//            // Step 2: Set sources
//            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//
//
//            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//            //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//            //    mMediaRecorder.setVideoSize(480,640);
//            // mMediaRecorder.setVideoFrameRate(10);
            mMediaRecorder.setVideoEncodingBitRate(256 * 8 * 1024);
            mMediaRecorder.setAudioEncodingBitRate(96 * 8 * 1024);
            mMediaRecorder.setOrientationHint(90);
//            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
//            // Step 4: Set output file
            String path = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();
            LogUtil.i("video_path", path);
            mMediaRecorder.setOutputFile(path);

//            // Step 5: Set the preview output
            mMediaRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());

            // Step 6: Prepare configured MediaRecorder
            try {
                mMediaRecorder.prepare();
                return true;
            } catch (IllegalStateException e) {
                Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
                releaseMediaRecorder();
                return false;
            } catch (IOException e) {
                Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
                releaseMediaRecorder();
                return false;
            }


        }
        return false;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Camera.Parameters parameters = c.getParameters();
            mOptimalPreviewSize = getOptimalPreviewSize(parameters.getSupportedVideoSizes(), 480, 640);
            parameters.setPreviewSize(mOptimalPreviewSize.width, mOptimalPreviewSize.height);
            c.setParameters(parameters);
            c.setDisplayOrientation(90);
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    Handler mHandler = new Handler();


    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.record) {
            if (isRecording) {
                stopRecording();
            } else {
                // initialize video camera
                if (prepareVideoRecorderAndDisplay()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    try {
                        mMediaRecorder.start();
                        mRecordBtn.setText("stop");
                        // inform the user that recording has started
                        //setCaptureButtonText("Stop");

                        isRecording = true;
                    } catch (Exception e) {
                        Toast.makeText(this, "暂时不支持您的手机摄像头", Toast.LENGTH_SHORT).show();
                        LogUtil.printStackTrace(e);
                    }

                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (isRecording) {
                                stopRecording();
                            }
                        }
                    }, 6 * 1000);
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorder();
                    // inform user
                }
            }
            LogUtil.i("record", "is recording:" + isRecording);
        }


    }

    private void stopRecording() {
        // stop recording and release camera
        mMediaRecorder.stop();  // stop the recording
        releaseMediaRecorder(); // release the MediaRecorder object
        mCamera.lock();         // take camera access back from MediaRecorder
        mRecordBtn.setText("record");
        // inform the user that recording has stopped
        //setCaptureButtonText("Capture");
        isRecording = false;
    }
}
