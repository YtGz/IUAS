/**
 * This class controls everything related with the camera and starts the catchBall Thread.
 * It extends the Main Activity and implements the Camera view Listener.
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class CameraFrameProcessingActivity extends MainActivity implements CvCameraViewListener2 {
    public static Mat                  		mRgba;
    private Scalar               			mBlobColorHsv;
    public static ColorBlobDetector    		mDetector;
    private Scalar               			CONTOUR_COLOR;
    private Scalar				 			POINT_COLOR;
    private Point				 			lowestTargetPoint;
    private final int						resolutionHeight = 320; //camera height resolution

    private CameraResolutionView		 			mOpenCvCameraView;
    public	static CatchBall			 	dfsm;
    public  static BallAndBeaconDetection 	ballDetection;
    public  static Odometry			 		localization;
    
    /**
     * Internal private class for starting OpenCV.
     */
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                	Utils.showLog("OpenCV loaded successfully");
                    mOpenCvCameraView.enableView(); // enable camera view
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Utils.showLog("called onCreate");
        super.onBallCatchingActivityCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.ball_catching_view);
        
        textLog = (TextView) findViewById(R.id.textLog);
        
        Intent intent = getIntent();
        double[] c = intent.getDoubleArrayExtra("mBlobColorHsv");
        mBlobColorHsv = new Scalar(c);
        Utils.showLog("mBlobColorHsv: " + mBlobColorHsv);

        mOpenCvCameraView = (CameraResolutionView) findViewById(R.id.ball_catching_activity_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
    }
    
    /**
     * On pause camera is disabled.
     */
    @Override
    public void onPause() {   
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    
    /**
     * Resume.
     */
    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }
    
    /**
     * Destroy and disable camera view.
     */
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    
    /**
     * Set camera view components on start.
     */
    public void onCameraViewStarted(int width, int height) {
    	Utils.showLog("Cam View: " + mOpenCvCameraView);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mDetector.setHsvColor(mBlobColorHsv);
        CONTOUR_COLOR = new Scalar(0,0,255,255);
        POINT_COLOR = new Scalar(255,0,0,255);
        for(Camera.Size s : mOpenCvCameraView.getResolutionList()) {
        	if(s.height == resolutionHeight) {
        		mOpenCvCameraView.setResolution(s);
        	}
        }
        //Self-localization using beacons
        localization = new Odometry();
        //camera processing
        ballDetection = new BallAndBeaconDetection();
        ballDetection.addListener(localization);
    }
    
    /**
     * "Release" mRgba variable when camera view stops.
     */
    public void onCameraViewStopped() {
        mRgba.release();
    }

    /**
     * Determines what happens on getting a camera frame.
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		mRgba = inputFrame.rgba();

		ArrayList<ArrayList<MatOfPoint>> l = ballDetection.detect(mRgba, mDetector);
		ArrayList<MatOfPoint> contours = l.get(0);
		ArrayList<MatOfPoint> lowestTargetPoint = l.get(1);
		Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);
		Imgproc.drawContours(mRgba, lowestTargetPoint, -1, POINT_COLOR);
        return mRgba;
    }
    
    /**
     * Finish on clicking the Back-Button.
     * 
     * @param view
     */
    public void backButtonOnClick(View view) {
        finish();
    }
    
    /**
     * Start the DFSM and environment observing processes
     * 
     * @param view
     */
    public void catchBallOnClick(View view){ 
    	//Obstacle avoidance
    	new Thread(new SensorData()).start();
    	//DFSM controlling the robot
        dfsm = new CatchBall();
        ballDetection.addListener(dfsm);
    }
    
}
