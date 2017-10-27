package cn.daizhe.opencv4androiddemo;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

import cn.daizhe.opencv4androiddemo.activity.PermissionsActivity;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final int REQUEST_CODE = 0; // 申请权限的请求码

    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    private String TAG = "OpenCV_Test";
    //OpenCV的相机接口
    private CameraBridgeViewBase mCVCamera;
    //缓存相机每帧输入的数据
    private Mat mRgba, mTmp;
    //按钮组件
    private Button mButton;
    //当前处理状态
    private static int Cur_State = 0;

    private Size mSize0;
    private Mat mIntermediateMat;
    private MatOfInt mChannels[];
    private MatOfInt mHistSize;
    private int mHistSizeNum = 25;
    private Mat mMat0;
    private float[] mBuff;
    private MatOfFloat mRanges;
    private Point mP1;
    private Point mP2;
    private Scalar mColorsRGB[];
    private Scalar mColorsHue[];
    private Scalar mWhilte;
    private Mat mSepiaKernel;

    /**
     * 通过OpenCV管理Android服务，异步初始化OpenCV
     */
    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mCVCamera.enableView();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // OpenCV摄像头组件
        mCVCamera = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        mCVCamera.setCvCameraViewListener(this);
        // 打开前置摄像头
        mCVCamera.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);

        mButton = (Button) findViewById(R.id.deal_btn);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Cur_State < 8) {
                    //切换状态
                    Cur_State++;
                } else {
                    //恢复初始状态
                    Cur_State = 0;
                }
            }
        });

        // 获取权限
        permissionSetting();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * 初始化OpenCV
     */
    private void initOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV library not found!");
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    /**
     * 权限设置
     */
    private void permissionSetting() {
        boolean isAllGranted = checkPermissionAllGranted(
                PERMISSIONS
        );
        // 如果这3个权限全都拥有, 则直接初始化OpenCV
        if (isAllGranted) {
            initOpenCV();
            return;
        }
        // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
        // 将自动弹出授权对话框
        ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                REQUEST_CODE
        );
    }

    /**
     * 检查授权状态
     *
     * @param permissions
     * @return
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }

    /**
     * 授权回掉函数
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            boolean isAllGranted = true;

            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (isAllGranted) {
                // 如果所有的权限都授予了, 则初始化OpenCV
                initOpenCV();

            } else {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                openAppDetails();
            }
        }
    }

    private void openAppDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("备份通讯录需要访问 “摄像头”权限，请到 “应用信息 -> 权限” 中授予！");
        builder.setPositiveButton("去手动授权", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拒绝时, 关闭页面, 缺少主要权限, 无法运行
        if (requestCode == REQUEST_CODE && resultCode == PermissionsActivity.PERMISSIONS_DENIED) {
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCVCamera != null) {
            mCVCamera.disableView();
        }
    }

    /**
     * 相机开始时调用
     * @param width  -  the width of the frames that will be delivered
     * @param height - the height of the frames that will be delivered
     */
    @Override
    public void onCameraViewStarted(int width, int height) {

        // TODO Auto-generated method stub
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mTmp = new Mat(height, width, CvType.CV_8UC4);

        mIntermediateMat = new Mat();
        mSize0 = new Size();
        mChannels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        mBuff = new float[mHistSizeNum];
        mHistSize = new MatOfInt(mHistSizeNum);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0 = new Mat();
        mColorsRGB = new Scalar[]{new Scalar(200, 0, 0, 255), new Scalar(0, 200, 0, 255), new Scalar(0, 0, 200, 255)};
        mColorsHue = new Scalar[]{
                new Scalar(255, 0, 0, 255), new Scalar(255, 60, 0, 255), new Scalar(255, 120, 0, 255), new Scalar(255, 180, 0, 255), new Scalar(255, 240, 0, 255),
                new Scalar(215, 213, 0, 255), new Scalar(150, 255, 0, 255), new Scalar(85, 255, 0, 255), new Scalar(20, 255, 0, 255), new Scalar(0, 255, 30, 255),
                new Scalar(0, 255, 85, 255), new Scalar(0, 255, 150, 255), new Scalar(0, 255, 215, 255), new Scalar(0, 234, 255, 255), new Scalar(0, 170, 255, 255),
                new Scalar(0, 120, 255, 255), new Scalar(0, 60, 255, 255), new Scalar(0, 0, 255, 255), new Scalar(64, 0, 255, 255), new Scalar(120, 0, 255, 255),
                new Scalar(180, 0, 255, 255), new Scalar(255, 0, 255, 255), new Scalar(255, 0, 215, 255), new Scalar(255, 0, 85, 255), new Scalar(255, 0, 0, 255)
        };
        mWhilte = Scalar.all(255);
        mP1 = new Point();
        mP2 = new Point();

        // Fill sepia kernel
        mSepiaKernel = new Mat(4, 4, CvType.CV_32F);
        mSepiaKernel.put(0, 0, /* R */0.189f, 0.769f, 0.393f, 0f);
        mSepiaKernel.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mSepiaKernel.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mSepiaKernel.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
    }

    @Override
    public void onCameraViewStopped() {
        // TODO Auto-generated method stub
        mRgba.release();
        mTmp.release();
    }

    /**
     * 图像处理都写在此处
     */
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        Size sizeRgba = mRgba.size();
        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;
        Mat rgbaInnerWindow;

        int left = cols / 8;
        int top = rows / 8;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;

        switch (Cur_State) {
            //case 1:
            //    //灰化处理
            //    Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            //    break;
            //case 2:
            //    //Canny边缘检测
            //    mRgba = inputFrame.rgba();
            //    Imgproc.Canny(inputFrame.gray(), mTmp, 80, 100);
            //    Imgproc.cvtColor(mTmp, mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            //    break;
            //case 3:
            //    //Hist直方图计算
            //    Mat hist = new Mat();
            //    int thikness = (int) (sizeRgba.width / (mHistSizeNum + 10) / 5);
            //    if (thikness > 5) thikness = 5;
            //    int offset = (int) ((sizeRgba.width - (5 * mHistSizeNum + 4 * 10) * thikness) / 2);
            //
            //    // RGB
            //    for (int c = 0; c < 3; c++) {
            //        Imgproc.calcHist(Arrays.asList(mRgba), mChannels[c], mMat0, hist, mHistSize, mRanges);
            //        Core.normalize(hist, hist, sizeRgba.height / 2, 0, Core.NORM_INF);
            //        hist.get(0, 0, mBuff);
            //        for (int h = 0; h < mHistSizeNum; h++) {
            //            mP1.x = mP2.x = offset + (c * (mHistSizeNum + 10) + h) * thikness;
            //            mP1.y = sizeRgba.height - 1;
            //            mP2.y = mP1.y - 2 - (int) mBuff[h];
            //            Imgproc.line(mRgba, mP1, mP2, mColorsRGB[c], thikness);
            //        }
            //    }
            //    // Value and Hue
            //    Imgproc.cvtColor(mRgba, mTmp, Imgproc.COLOR_RGB2HSV_FULL);
            //    // Value
            //    Imgproc.calcHist(Arrays.asList(mTmp), mChannels[2], mMat0, hist, mHistSize, mRanges);
            //    Core.normalize(hist, hist, sizeRgba.height / 2, 0, Core.NORM_INF);
            //    hist.get(0, 0, mBuff);
            //    for (int h = 0; h < mHistSizeNum; h++) {
            //        mP1.x = mP2.x = offset + (3 * (mHistSizeNum + 10) + h) * thikness;
            //        mP1.y = sizeRgba.height - 1;
            //        mP2.y = mP1.y - 2 - (int) mBuff[h];
            //        Imgproc.line(mRgba, mP1, mP2, mWhilte, thikness);
            //    }
            //    break;
            //case 4:
            //    //Sobel边缘检测
            //    Mat gray = inputFrame.gray();
            //    Mat grayInnerWindow = gray.submat(top, top + height, left, left + width);
            //    rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width);
            //    Imgproc.Sobel(grayInnerWindow, mIntermediateMat, CvType.CV_8U, 1, 1);
            //    Core.convertScaleAbs(mIntermediateMat, mIntermediateMat, 10, 0);
            //    Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
            //    grayInnerWindow.release();
            //    rgbaInnerWindow.release();
            //    break;
            //case 5:
            //    //SEPIA(色调变换)
            //    rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width);
            //    Core.transform(rgbaInnerWindow, rgbaInnerWindow, mSepiaKernel);
            //    rgbaInnerWindow.release();
            //    break;
            //case 6:
            //    //ZOOM放大镜
            //    Mat zoomCorner = mRgba.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10);
            //    Mat mZoomWindow = mRgba.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100);
            //    Imgproc.resize(mZoomWindow, zoomCorner, zoomCorner.size());
            //    Size wsize = mZoomWindow.size();
            //    Imgproc.rectangle(mZoomWindow, new Point(1, 1), new Point(wsize.width - 2, wsize.height - 2), new Scalar(255, 0, 0, 255), 2);
            //    zoomCorner.release();
            //    mZoomWindow.release();
            //    break;
            //case 7:
            //    //PIXELIZE像素化
            //    rgbaInnerWindow = mRgba.submat(top, top + height, left, left + width);
            //    Imgproc.resize(rgbaInnerWindow, mIntermediateMat, mSize0, 0.1, 0.1, Imgproc.INTER_NEAREST);
            //    Imgproc.resize(mIntermediateMat, rgbaInnerWindow, rgbaInnerWindow.size(), 0., 0., Imgproc.INTER_NEAREST);
            //    rgbaInnerWindow.release();
            //    break;
            default:
                //显示原图
                mRgba = inputFrame.rgba();
                break;
        }
        //返回处理后的结果数据
        return mRgba;
    }
}