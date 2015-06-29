package com.shinchven.simplecamera.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.IOException;
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

    public class CompressTask extends AsyncTask {

        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mDialog = new ProgressDialog(CameraActivity.this);
            try {
                mDialog.show();
            } catch (Exception e) {
                LogUtil.printStackTrace(e);
            }
        }

        @Override
        protected Object doInBackground(Object[] params) {
            FFmpeg ffmpeg = FFmpeg.getInstance(CameraActivity.this);

            try {
                ffmpeg.loadBinary(new LoadBinaryResponseHandler() {
                    @Override
                    public void onFailure() {
                        //showUnsupportedExceptionDialog();
                    }
                });
            } catch (FFmpegNotSupportedException e) {
                // showUnsupportedExceptionDialog();
            }


            DisplayUtil.DisplayMatrix matrix = DisplayUtil.getScreenDisplayMatrix(CameraActivity.this);

            int cropWidth2 = matrix.width;
            int cropHeight2 = matrix.width / 4 * 3;
            int top2 = (matrix.height - cropHeight2) / 2;

            Camera.Size bestRecordSize = getOptimalPreviewSize(mCamera.getParameters().getSupportedVideoSizes(), 10000, 10000);

            int cropWidth = bestRecordSize.height;
            int cropHeight = bestRecordSize.height / 4 * 3;
            int top = (bestRecordSize.width - cropHeight) / 2;


            try {
                // to execute "ffmpeg -version" command you just need to pass "-version"
//                String cmd = "ffmpeg -i " + Storage.getOutputMediaFile() + " -vcodec libx264 -crf 20 " + Storage.getOutputCompressedMediaFile();
                String cmd = "-i " + Storage.getOutputMediaFile().getAbsolutePath() +
                        " -strict -2 -codec:v mpeg4 -b:v 512k -aspect 3:4 -vf crop=" + cropHeight + ":" + cropWidth + ":" + top + ":" + 0 + ",scale=640:480 "
                        + Storage.getOutputCompressedMediaFile().getAbsolutePath();
                ffmpeg.execute(cmd, new ExecuteBinaryResponseHandler() {

                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onProgress(String message) {
                        LogUtil.i(message);
                    }

                    @Override
                    public void onFailure(String message) {
                        LogUtil.i(message);
                    }

                    @Override
                    public void onSuccess(String message) {
                        LogUtil.i(message);
                    }

                    @Override
                    public void onFinish() {
                        LogUtil.i("finished");
                    }
                });
            } catch (FFmpegCommandAlreadyRunningException e) {
                // Handle if FFmpeg is already running
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            try {
                mDialog.dismiss();
            } catch (Exception e) {
                LogUtil.printStackTrace(e);
            }

            File compressedMediaFile = Storage.getOutputCompressedMediaFile();
            if (compressedMediaFile.exists()) {
                Intent intent = new Intent(CameraActivity.this, VideoViewActivity.class);
                intent.putExtra(VideoViewActivity.KEY_PATH, compressedMediaFile.getAbsolutePath());
                startActivity(intent);
            }
            super.onPostExecute(o);
        }
    }

    private boolean prepareVideoRecorderAndDisplay() {
        if (mCamera != null) {
            mMediaRecorder = new MediaRecorder();

            // Step 1: Unlock and set camera to MediaRecorder
            try {
                mCamera.unlock();
                mMediaRecorder.setCamera(mCamera);

//                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
//                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//                mMediaRecorder.setVideoSize(1280,720);
//                mMediaRecorder.setVideoFrameRate(30);
//                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

//            // Step 2: Set sources
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

//
//            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
                mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
//            //mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
////            //    mMediaRecorder.setVideoSize(480,640);
////            // mMediaRecorder.setVideoFrameRate(10);
                //   mMediaRecorder.setVideoEncodingBitRate(128 * 8 * 1024);
                // mMediaRecorder.setAudioEncodingBitRate(1 * 8 * 1024);
                // mMediaRecorder.setMaxFileSize(1*1024*1024);
                mMediaRecorder.setOrientationHint(90);
//            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
//            // Step 4: Set output file
                String path = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();
                LogUtil.i("video_path", path);
                mMediaRecorder.setOutputFile(Storage.getOutputMediaFile().getAbsolutePath());

//            // Step 5: Set the preview output
                mMediaRecorder.setPreviewDisplay(mCameraPreview.getHolder().getSurface());
            } catch (IllegalStateException e) {
                LogUtil.printStackTrace(e);
            }

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
            parameters.set("orientation", "portrait");
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
                // stopRecording();
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
                    }, 8 * 1000);
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
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(CameraActivity.this);
                builder.setMessage("播放视频");
                builder.setPositiveButton("源视频", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(CameraActivity.this, VideoViewActivity.class);
                        intent.putExtra(VideoViewActivity.KEY_PATH, Storage.getOutputMediaFile().getAbsolutePath());
                        startActivity(intent);
                    }
                });
                builder.setNegativeButton("压缩视频", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Storage.getOutputCompressedMediaFile().exists()) {
                            Storage.getOutputCompressedMediaFile().delete();
                        }
                        new CompressTask().execute();
                    }
                });
                try {
                    builder.show();
                } catch (Exception e) {
                    LogUtil.printStackTrace(e);
                }


            }
        }, 1000);
    }
}
