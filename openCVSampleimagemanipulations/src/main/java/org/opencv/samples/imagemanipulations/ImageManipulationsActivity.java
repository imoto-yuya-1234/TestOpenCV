package org.opencv.samples.imagemanipulations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.graphics.Paint;
import android.widget.RelativeLayout;

public class ImageManipulationsActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG                 = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;

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


    Ball ball;
    Handler handler;
    int width, height;
    int dx = 10, dy = 10, time = 100;
    private Runnable runnable;

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

        // ボールの描画
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        android.graphics.Point point = new android.graphics.Point();
        display.getSize(point);
        width = point.x; //画面の幅
        height = point.y; //画面の高さ
        ball = new Ball(this);
        ball.x = width / 2; //ここで
        ball.y = height / 2; //ボールの位置を指定
        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                ball.x += dx;
                ball.y -= dy;

                if (ball.x <= ball.radius) {
                    ball.x = ball.radius;
                    dx = -dx;
                } else if (ball.x >= width - ball.radius) {
                    ball.x = width - ball.radius;
                    dx = -dx;
                }

                if (ball.y <= ball.radius) {
                    ball.y = ball.radius;
                    dy = -dy;
                } else if (ball.y >= height - ball.radius) {
                    ball.y = height - ball.radius;
                    dy = -dy;
                }

                // Viewの再描画
                ball.invalidate();
                handler.postDelayed(runnable, time);
            }
        };
        handler.postDelayed(runnable, time);

        addContentView(ball, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
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

    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat rgbaMat = inputFrame.rgba();
        Mat grayMat = inputFrame.gray();

        Square2Rect(grayMat, rgbaMat);
        grayMat.release();

        return rgbaMat;
    }

    private void Square2Rect(Mat inMat, Mat outMat) {
        Mat corMat = inMat.clone();
        int width = corMat.width();
        int height = corMat.height();

        Imgproc.threshold(corMat, corMat, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(corMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);

        corMat.release();

        // search max bule area
        int index = -1;
        double contourArea = 0;
        for (int i = 0; i < contours.size(); i++) {
            double tmpArea = Imgproc.contourArea(contours.get(i));
            if(contourArea < tmpArea) {
                contourArea = tmpArea;
                index = i;
            }
        }

        if(index != -1) {
            MatOfPoint2f contours2f = new MatOfPoint2f( contours.get(index).toArray() );
            MatOfPoint2f approx2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(contours2f, approx2f, 0.05 * Imgproc.arcLength(contours2f, true), true);

            if (approx2f.height() == 4) {
                boolean pointOK[] = new boolean[]{false, false, false, false};
                float srcPoint[] = new float[]{0, 0, width, 0, width, height, 0, height};

                for (int k = 0; k < approx2f.height(); k++) {
                    double[] m = approx2f.get(k, 0);
                    float pointX = (float)m[0];
                    float pointY = (float)m[1];
                    int l = 0;
                    if (pointX < width / 2 && pointY < height / 2) {
                        l = 0;
                    } else if (pointX > width / 2 && pointY < height / 2) {
                        l = 1;
                    } else if (pointX > width / 2 && pointY > height / 2) {
                        l = 2;
                    } else if (pointX < width / 2 && pointY > height / 2) {
                        l = 3;
                    }
                    pointOK[l] = true;
                    srcPoint[2*l] = pointX;
                    srcPoint[2*l + 1] = pointY;
                }

                if (pointOK[0] && pointOK[1] && pointOK[2] && pointOK[3]) {
                    float xMargin = width / 8;
                    float yMargin = height / 8;
                    float dstPoint[] = new float[]{xMargin, yMargin, width - xMargin, yMargin, width - xMargin, height - yMargin, xMargin, height - yMargin};
                    matPerspectiveTransform(outMat, outMat, srcPoint, dstPoint);
                }
            }
        }
    }

    private void matPerspectiveTransform(Mat inMat, Mat outMat, float srcPoint[], float dstPoint[]) {
        Mat srcPointMat = new Mat(4,2,CvType.CV_32F);
        srcPointMat.put(0, 0, srcPoint);

        Mat dstPointMat = new Mat(4,2,CvType.CV_32F);
        dstPointMat.put(0, 0, dstPoint);

        //変換行列作成
        Mat r_mat = Imgproc.getPerspectiveTransform(srcPointMat, dstPointMat);

        //図形変換処理
        Imgproc.warpPerspective(inMat, outMat, r_mat, outMat.size(),Imgproc.INTER_LINEAR);
    }

    private void DrwLines(Mat lines, Mat img) {
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
    private void DrwCircles(Mat circles, Mat img) {
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
}
