package org.opencv.samples.imagemanipulations;

import java.util.Arrays;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
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
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.graphics.Paint;

public class ImageManipulationsActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat                  mIntermediateMat;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public ImageManipulationsActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.image_manipulations_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.image_manipulations_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat InImage = inputFrame.rgba();
        Mat GrayImage = inputFrame.gray();
        Size FrameSize = GrayImage.size();
        int height = (int) FrameSize.height;
        int width = (int) FrameSize.width;

        Mat OutImage = new Mat();
        Mat HsvImage = new Mat();
        Mat HsvOutImage = new Mat();
        Mat rgbaImage = new Mat();
        Mat rgba2Image = new Mat();
        Mat SmoothImage;
        Mat CannyImage;
        Mat Circles = new Mat();
        Mat Lines = new Mat();
        SmoothImage = GrayImage.submat(0, height, 0, width);
        CannyImage = GrayImage.submat(0, height, 0, width);

        //Imgproc.GaussianBlur(GrayImage, SmoothImage, new Size(3,3), 2.0, 2.0);
        //Imgproc.Canny(SmoothImage, CannyImage, 100, 150);

        Imgproc.cvtColor(InImage, HsvImage, Imgproc.COLOR_BGR2HSV, 3);
        Core.inRange(HsvImage, new Scalar(90, 50, 50), new Scalar(125, 255, 255), HsvOutImage);
        Imgproc.cvtColor(HsvOutImage, rgbaImage, Imgproc.COLOR_GRAY2BGR, 0);
        Imgproc.cvtColor(rgbaImage, rgba2Image, Imgproc.COLOR_BGR2RGBA, 0);


        //Imgproc.HoughLinesP(CannyImage, Lines, 1, Math.PI/180, 50, 100, 50);
        //fncDrwLines(Lines, InImage);
        //Imgproc.HoughCircles(CannyImage, Circles, Imgproc.CV_HOUGH_GRADIENT, 2, 50, 160, 100, 100, 200);
        //fncDrwCircles(Circles, InImage);

        //GrayImage.release();
        //SmoothImage.release();

        //Imgproc.rectangle(InImage, new Point(0, 0), new Point(width, height), new Scalar(0, 0, 255), 10);

        return OutImage;
    }

    private void fncDrwCircles(Mat circles, Mat img) {
        double[] data;
        double rho;
        Point pt = new Point();
        for (int i = 0; i < circles.cols(); i++){
            data = circles.get(0, i);
            pt.x = data[0];
            pt.y = data[1];
            rho = data[2];
            Imgproc.circle(img, pt, (int)rho, new Scalar(255, 0, 0), -1);
        }
    }

    private void fncDrwLines(Mat lines, Mat img) {
        double[] data;
        Point pt1 = new Point();
        Point pt2 = new Point();
        for (int i = 0; i < lines.cols(); i++){
            data = lines.get(0, i);
            pt1.x = data[0];
            pt1.y = data[1];
            pt2.x = data[2];
            pt2.y = data[3];
            Imgproc.line(img, pt1, pt2, new Scalar(255, 0, 0), 5);
        }
    }

}
