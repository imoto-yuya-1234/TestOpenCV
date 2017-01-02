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
        Mat rgbaImage = inputFrame.rgba();
        Mat grayImage = inputFrame.gray();
        Size FrameSize = rgbaImage.size();
        int height = (int) FrameSize.height;
        int width = (int) FrameSize.width;

        //Mat dstMat = new Mat(rgbaImage.rows(), rgbaImage.cols(), rgbaImage.type());
        Mat dstMat = rgbaImage.clone();

        Mat roi = new Mat();
        //roi = InImage.submat(0, height, 0, width);
        grayImage.copyTo(roi);
        Mat roiTmp = roi.clone();

        Imgproc.threshold(roiTmp, roiTmp, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(roiTmp, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);

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
                    for (int l = 0; l < approx2f.height(); l++) {
                        Scalar color;
                        if (l == 0) {
                            color = new Scalar(255, 0, 0);
                        } else if (l == 1) {
                            color = new Scalar(0, 255, 0);
                        } else if (l == 2) {
                            color = new Scalar(0, 0, 255);
                        } else {
                            color = new Scalar(255, 255, 0);
                        }
                        Imgproc.circle(dstMat, new Point(srcPoint[2*l], srcPoint[2*l + 1]), 10, color, -1);
                    }

                    dstMat = CorrectImage(dstMat, srcPoint);
                }
            }
        }
        return dstMat;
    }

    private Mat CorrectImage(Mat inImage, float srcPoint[]) {
        int width = inImage.width();
        int height = inImage.height();

        //float srcPoint[] = new float[]{0, 0, width, 0, width, height, 0, height};
        Mat srcPointMat = new Mat(4,2,CvType.CV_32F);
        srcPointMat.put(0, 0, srcPoint);

        float xMargin = width / 8;
        float yMargin = height / 8;
        float dstPoint[] = new float[]{xMargin, yMargin, width - xMargin, yMargin, width - xMargin, height - yMargin, xMargin, height - yMargin};
        Mat dstPointMat = new Mat(4,2,CvType.CV_32F);
        dstPointMat.put(0, 0,dstPoint );

        //変換行列作成
        Mat r_mat = Imgproc.getPerspectiveTransform(srcPointMat, dstPointMat);
        //図形変換処理
        Mat outImage = new Mat(inImage.rows(), inImage.cols(), inImage.type());
        Imgproc.warpPerspective(inImage, outImage, r_mat, outImage.size(),Imgproc.INTER_LINEAR);

        return outImage;
    }

    private void fncDrwLines(Mat lines, Mat img) {
        double[] data;
        Point pt1 = new Point();
        Point pt2 = new Point();
        Log.d("houghlineC", "hough line count = "+lines.cols()+"");
        for (int i = 0; i < lines.cols(); i++){
            data = lines.get(0, i);
            pt1.x = data[0];
            pt1.y = data[1];
            pt2.x = data[2];
            pt2.y = data[3];
            Imgproc.line(img, pt1, pt2, new Scalar(255, 0, 0), 5);
        }
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
}
