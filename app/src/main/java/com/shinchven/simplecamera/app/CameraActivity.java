package com.shinchven.simplecamera.app;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.util.List;


public class CameraActivity extends AppCompatActivity {

    private Camera mCamera;
    private FrameLayout mCameraPreviewContainer;
    private CameraPreview mCameraPreview;
    private DisplayUtil.DisplayMatrix mScreenDisplayMatrix;
    private Camera.Size mOptimalPreviewSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (!checkCameraHardware(this)) {
            Toast.makeText(this, "your camera is not ready..", Toast.LENGTH_SHORT);
            finish();
            return;
        }

        mScreenDisplayMatrix = DisplayUtil.getScreenDisplayMatrix(this);
        mCameraPreviewContainer = ((FrameLayout) findViewById(R.id.camera_preview_container));
    }

    @Override
    protected void onResume() {
        super.onResume();

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

    @Override
    protected void onStop() {
        super.onStop();
        mCamera.release();
        mCamera = null;
    }

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            Camera.Parameters parameters = c.getParameters();
            mOptimalPreviewSize = getOptimalPreviewSize(parameters.getSupportedVideoSizes(), 720, 720);
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
}
