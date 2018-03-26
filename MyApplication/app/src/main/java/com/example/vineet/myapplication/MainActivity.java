package com.example.cjsan.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import static com.example.cjsan.myapplication.R.*;

public class MainActivity extends AppCompatActivity implements OnTouchListener,CvCameraViewListener2, SeekBar.OnSeekBarChangeListener{



    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat mRgba;

    private Scalar mBlobColorRgba;
    private Scalar mBlobColorHsv;
    double x = -1;
    double y = -1;
    int dim = 20;
    TextView touch_coordinates;
    TextView touch_color;
    private boolean flag;
    private int b_val;
    private int s_val;
    private String operation;
    private Scalar c= new Scalar(20);
    private List<Rect>  ListOfRect_s = new ArrayList<Rect>();
    private List<Rect>  ListOfRect_b = new ArrayList<Rect>();
    private List<Rect>  ListOfRect   = new ArrayList<Rect>();

    private AppCompatSeekBar sb_brightness;
    private AppCompatSeekBar sb_sharpness;

    // private Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        touch_coordinates=(TextView)findViewById(id.touch_coordinates);
        touch_color = (TextView)findViewById(id.touch_color);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.opencv_tutorial_activity_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        initViews();

    }

    private void initViews(){

        sb_brightness = (AppCompatSeekBar)findViewById(R.id.sb_brightness);

        sb_brightness.setOnSeekBarChangeListener(this);

        sb_sharpness = (AppCompatSeekBar)findViewById(R.id.sb_sharpness);

        sb_sharpness.setOnSeekBarChangeListener(this);
    }


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    //Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

//    static {
//        System.loadLibrary("MyLibs");
//    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0,this,mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        double yLow   = (double)mOpenCvCameraView.getHeight() * 0.2401961;
        double yHigh  = (double)mOpenCvCameraView.getHeight() * 0.7696078;
        double xScale = (double)cols / (double)mOpenCvCameraView.getWidth();
        double yScale = (double)rows / (yHigh - yLow);
        x = event.getX();
        y = event.getY();
        y = y - yLow;
        x = x * xScale;
        y = y * yScale;

        if((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        touch_coordinates.setText("X: " + Double.valueOf((int)x) + ", Y: " + Double.valueOf((int)y));
        Rect touchedRect = new Rect();

        touchedRect.x = (int)x;
        touchedRect.y = (int)y;

        touchedRect.width = dim;
        touchedRect.height = dim;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);
        Mat touchedRegionHsv = new Mat();

        Imgproc.cvtColor(touchedRegionRgba,touchedRegionHsv,Imgproc.COLOR_RGB2HSV_FULL);
        Imgproc.cvtColor(touchedRegionRgba,touchedRegionHsv,Imgproc.COLOR_RGB2HSV_FULL);

        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width * touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;
        mBlobColorRgba = convertScalarHsv2Rgba(mBlobColorHsv);

        touch_color.setText(
            "Color: #"
            + String.format("%02X", (int)mBlobColorRgba.val[0])
            + String.format("%02X", (int)mBlobColorRgba.val[1])
            + String.format("%02X", (int)mBlobColorRgba.val[2])
        );

        touch_color.setTextColor(
                Color.rgb((int)mBlobColorRgba.val[0],
                        (int)mBlobColorRgba.val[1],
                        (int)mBlobColorRgba.val[2])
        );


        Rect touchedRectBlur = new Rect();
        touchedRectBlur.x = (int)x;
        touchedRectBlur.y = (int)y;

        touchedRectBlur.width=11;
        touchedRectBlur.height=11;
        flag = true;



        return false;
    }

    private Scalar convertScalarHsv2Rgba (Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0,0));
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
    }


    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();


            for(int h=0;h<s_val/10;h++){
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(mRgba, blurred, new Size(5,5), 0);
        Core.addWeighted(mRgba,1.5, blurred, -0.5, 0, blurred);
        blurred.copyTo(mRgba);// Does seem to fail when the image gets too noisy
        blurred.release();
            }


        mRgba.convertTo(mRgba,-1,1,b_val);
    return mRgba;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if(seekBar.equals(sb_brightness)){
        b_val=progress;
        }
        if(seekBar.equals(sb_sharpness)){
            s_val=progress;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }




    }

 /* Increasing sharpness reference : https://stackoverflow.com/questions/37750222/increase-constrast-and-brightness-of-a-grayscale-image-in-opencv-for-android*/