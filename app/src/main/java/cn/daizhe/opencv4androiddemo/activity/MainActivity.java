package cn.daizhe.opencv4androiddemo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import cn.daizhe.opencv4androiddemo.R;

import static org.opencv.imgproc.Imgproc.warpAffine;

//import android.support.v7.app.AppCompatActivity;
//import android.content.Context;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private static final String TAG = "OCVSample::Activity";
    private boolean mIsFrontCamera = false;
    //ImageView picture;
    Mat imageMat;
    Mat mRgba;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(MainActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    imageMat = new Mat();

                    //加载人脸检测xml
                    try {
                        // Copy the resource into a temp file so OpenCV can load it
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        // Load the cascade classifier
                        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                    } catch (Exception e) {
                        Log.e("OpenCVActivity", "Error loading cascade", e);
                    }

                    //enable camera
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + MainActivity.this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //picture   = (ImageView)findViewById(R.id.imageView);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(MainActivity.this);

        Button button1 = (Button) findViewById(R.id.button);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "开启前置相机!", Toast.LENGTH_SHORT).show();
                mOpenCvCameraView.setVisibility(SurfaceView.GONE);

                //前置相机
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
                mOpenCvCameraView.setCameraIndex(1);

                mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
                mOpenCvCameraView.setCvCameraViewListener(MainActivity.this);
                mOpenCvCameraView.enableView();
                mIsFrontCamera = true;
            }
        });

        Button button2 = (Button) findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "开启后置相机!!", Toast.LENGTH_SHORT).show();
                mOpenCvCameraView.setVisibility(SurfaceView.GONE);

                //后置相机
                mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_surface_view);
                mOpenCvCameraView.setCameraIndex(-1);

                mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
                mOpenCvCameraView.setCvCameraViewListener(MainActivity.this);
                mOpenCvCameraView.enableView();
                mIsFrontCamera = false;
            }
        });
    }

    //@Override
    //public boolean onCreateOptionsMenu(Menu menu){
    //    getMenuInflater().inflate(R.menu.menu_main, menu);
    //    return true;
    //}
    //
    //@Override
    //public boolean onOptionsItemSelected(MenuItem item){
    //    switch(item.getItemId()){
    //        case R.id.add_item:
    //            Toast.makeText(this, "You Clicked Add!!", Toast.LENGTH_SHORT).show();
    //            break;
    //        case R.id.remove_item:
    //            Toast.makeText(this, "You Clicked Remove!", Toast.LENGTH_SHORT).show();
    //            break;
    //        case R.id.delete_item:
    //            Toast.makeText(this, "You Clicked Delete!", Toast.LENGTH_SHORT).show();
    //            break;
    //        default:
    //            break;
    //    }
    //    return true;
    //}


    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mGray = inputFrame.gray();
        Mat mShow;
        mShow = inputFrame.rgba();
        if (!mIsFrontCamera) {
            Core.flip(mShow, mShow, 1);
        }

        //if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
        //    Log.i(TAG, "shuping");
        //    Mat rotateMat = Imgproc.getRotationMatrix2D(new Point(mGray.rows() / 2, mGray.cols() / 2), 90, 1);
        //    warpAffine(mGray, mGray, rotateMat, mGray.size());
        //}

        //检测并显示
        MatOfRect faces = new MatOfRect();
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(mGray, faces, 1.1, 2, 2, new Size(mGray.height() / 5, mGray.height() / 5), new Size());
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++)
            //if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            //    Imgproc.rectangle(mShow, facesArray[i].br(), facesArray[i].tl(), new Scalar(0, 255, 0, 255), 3);
            //    Log.i(TAG, "shuping");
            //} else {
                Imgproc.rectangle(mShow, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);
            //    Log.i(TAG, "hengping");
            //}
        return mShow;
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

}