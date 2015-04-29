package com.example.iuas;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class BallCatchingActivity extends MainActivity implements CvCameraViewListener2 {
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private Scalar				 POINT_COLOR;
    private Mat				 	 homography;
    private Point				 lowestTargetPoint;

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

    public BallCatchingActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	homography = ColorBlobDetectionActivity.homography;
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.ball_catching_view);
        
        Intent intent = getIntent();
        double[] c = intent.getDoubleArrayExtra("mBlobColorRgba");
        mBlobColorRgba = new Scalar(c);


        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.ball_catching_activity_view);
        
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
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(0,0,255,255);
        POINT_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
	        mRgba = inputFrame.rgba();
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            Log.e(TAG, "Contours count: " + contours.size());
            for (MatOfPoint mp : contours) {
            	double min = Double.MAX_VALUE;
            	for (Point p : mp.toArray()) {
            		if(p.y < min){
            			min = p.y;
            			lowestTargetPoint = p;
            		}      		
            	}
            	break;
            	
            }
            ArrayList<MatOfPoint> l = new ArrayList<MatOfPoint>();
            l.add(new MatOfPoint(lowestTargetPoint));
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
            Imgproc.drawContours(mRgba, l, -1, POINT_COLOR);
            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);
            
            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
        return mRgba;
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
    
    
    public void backButtonOnClick(View view) {
    	Intent intent = new Intent();
        intent.putExtra("color", mBlobColorRgba.val);
        setResult(RESULT_OK, intent);
        finish();
    }
    
    public void catchBallOnClick(View view){
    	Point p = convertImageToGround(lowestTargetPoint);
    	navigateIgnoringObstacles((int)p.x, (int)p.y, 0);
    }
    
    public Point convertImageToGround(Point p){
    	Mat src =  new Mat(1, 1, CvType.CV_32FC2);
        Mat dest = new Mat(1, 1, CvType.CV_32FC2);
        src.put(0, 0, new double[] { p.x, p.y }); // p is a point in image coordinates
        Core.perspectiveTransform(src, dest, homography); //homography is your homography matrix
        Point dest_point = new Point(dest.get(0, 0)[0], dest.get(0, 0)[1]);
       // System.out.println(dest_point);
        return dest_point;
    }
}
