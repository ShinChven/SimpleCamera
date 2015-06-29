package com.shinchven.simplecamera.app;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private Button mBtnOpenCameraActivity;
    private Button mBtnOpenVideoViewActivity;
    private Button mBtnEncode;
    private Button mBtnPlayEncoded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnOpenCameraActivity = ((Button) findViewById(R.id.btn_open_camera_activity));
        mBtnOpenVideoViewActivity = (Button) findViewById(R.id.btn_open_video_view_activity);
        mBtnEncode = (Button) findViewById(R.id.btn_encode);
        mBtnEncode.setOnClickListener(this);
        mBtnPlayEncoded = (Button) findViewById(R.id.btn_play_encoded);
        mBtnPlayEncoded.setOnClickListener(this);
        mBtnOpenVideoViewActivity.setOnClickListener(this);
        mBtnOpenCameraActivity.setOnClickListener(this);
        ((TextView) findViewById(R.id.text)).setShadowLayer(4, 5, 5, Color.BLACK);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public class CompressTask extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] params) {
            FFmpeg ffmpeg = FFmpeg.getInstance(MainActivity.this);

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


            DisplayUtil.DisplayMatrix matrix = DisplayUtil.getScreenDisplayMatrix(MainActivity.this);
            int cropWidth = matrix.width;
            int cropHeight = matrix.width / 4 * 3;
            int top = (matrix.height - cropHeight) / 2;

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

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_open_camera_activity) {
            startActivity(new Intent(this, CameraActivity.class));
        } else if (id == R.id.btn_open_video_view_activity) {
            Intent intent = new Intent(this, VideoViewActivity.class);
            intent.putExtra(VideoViewActivity.KEY_PATH, Storage.getOutputMediaFile().getAbsolutePath());
            startActivity(intent);
        } else if (id == R.id.btn_encode) {
            if (Storage.getOutputCompressedMediaFile().exists()) {
                Storage.getOutputCompressedMediaFile().delete();
            }
            new CompressTask().execute();
        } else if (id == R.id.btn_play_encoded) {
            Intent intent = new Intent(this, VideoViewActivity.class);
            intent.putExtra(VideoViewActivity.KEY_PATH, Storage.getOutputCompressedMediaFile().getAbsolutePath());
            startActivity(intent);
        }
    }
}
