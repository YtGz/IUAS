/**
 * This class provides methods to explore, find and catch a ball in workspace.
 * It extends the Main Activity.
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jp.ksksue.driver.serial.FTDriver;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class CopyOfBallCatchingActivity extends MainActivity implements CvCameraViewListener2, Runnable {
    private Mat                  mRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Scalar               CONTOUR_COLOR;
    private Scalar				 POINT_COLOR;
    private Mat				 	 homography;
    private Point				 lowestTargetPoint;
    private Point				 robotPosition;
    private static double		 robotRotation;

    private CameraBridgeViewBase mOpenCvCameraView;
    
    /**
     * Internal private class for starting OpenCV.
     */
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                	showLog("OpenCV loaded successfully");
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
     * Constructor.
     */
    public CopyOfBallCatchingActivity() {
        showLog("Instantiated new " + this.getClass());
    }

    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("called onCreate");
        super.onBallCatchingActivityCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.ball_catching_view);
        
        textLog = (TextView) findViewById(R.id.textLog);
        
        Intent intent = getIntent();
        double[] c = intent.getDoubleArrayExtra("mBlobColorHsv");
        mBlobColorHsv = new Scalar(c);
        System.out.println("mBlobColorHsv: " + mBlobColorHsv);
        homography = ColorBlobDetectionActivity.homography;
        robotPosition = new Point(0, 0);
        robotRotation = 0;

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.ball_catching_activity_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
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
    	showLog("Cam View: " + mOpenCvCameraView);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mDetector.setHsvColor(mBlobColorHsv);
        CONTOUR_COLOR = new Scalar(0,0,255,255);
        POINT_COLOR = new Scalar(255,0,0,255);
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
	    mDetector.process(mRgba);
	    List<MatOfPoint> contours = mDetector.getContours();
	   // System.out.println("Contours count: " + contours.size());
	    if(contours.size() > 0) {
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
	    }    
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
     * Converts image to ground (calculates lowest target point) and returns a destination point.
     * 
     * @param p
     * @return
     */
    public Point convertImageToGround(Point p){
    	Mat src =  new Mat(1, 1, CvType.CV_32FC2);
        Mat dest = new Mat(1, 1, CvType.CV_32FC2);
        src.put(0, 0, new double[] { p.x, p.y }); // p is a point in image coordinates
        Core.perspectiveTransform(src, dest, homography); //homography is the homography matrix
        Point dest_point = new Point(dest.get(0, 0)[0], dest.get(0, 0)[1]);
       // showLog("convertImageToGround: dest_point: " + dest_point);
        return dest_point;
    }
    
    
    
    /**
     * Run method of new thread which is called on thread start of ball catching.
     */

	
	/**
	 * This method checks if the ball was found by using the caluclated lowest target point.
	 */
	private void detectedBall() {
		//work();
		Point p = convertImageToGround(lowestTargetPoint);
		System.out.println("lowest target point: "+p);
		p.x /= 10;
		p.y = p.y/10-15;
		double[] d = cartesianToPolar(p);
		System.out.println("polar to target point: " + d[1] + " " + d[0]);
		/*robotMove(d[1], d[0], false);
		robotMove(0, -5, false);*/
		robotSetBar((byte) 0);
		robotFlashLed(0);
	}
	
	/**
	 * Converts coordinates from polar to cartesian.
	 * 
	 * @param p
	 * @return
	 */
	public Point polarToCartesian(double phi, double r) {
		double xf = Math.cos(Math.toRadians(phi)) * r;
		double yf = Math.sin(Math.toRadians(phi)) * r;
		
		return new Point(xf, yf);
	}
	
	/**
	 * Converts coordinates from cartesian to polar.
	 * 
	 * @param p
	 * @return
	 */
	public static double[] cartesianToPolar(Point p){
		double r =  Math.sqrt(p.x*p.x + p.y * p.y);
    	double phi =  Math.toDegrees(Math.atan2(p.y, p.x));
    	return new double[]{r, phi};
	}
	

	
	/**
	 * Explores workspace ala "Zick-Zack".
	 * Makes one turn and one driving distance per method call.
	 * 
	 * @see local sheets where this path was drawn & calculated
	 */
	/*
	public void exploreWorkspace() {
		final int workspaceFactor = 1;
		final double density = Math.sqrt(17);  	//Math.sqrt(5); for 2 crossings / Math.sqrt(17); for 4 crossings / Math.sqrt(65); for 8 crossings
		if(robotMove(-45, Math.sqrt(45000)/workspaceFactor, true)) return;
		if(robotMove(135, 300/workspaceFactor, true)) return;
		if(robotMove(Math.ceil((180 - (Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
		if(robotMove(-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density)))))), Math.sqrt(95625)/workspaceFactor, true)) return;
		if(robotMove(Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
		if(robotMove(-Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
		if(robotMove(Math.ceil(180 - (Math.asin(Math.toRadians(75/density)))), 300/workspaceFactor, true)) return;
		robotMove(135, Math.sqrt(45000)/workspaceFactor, true);	
	}*/
	
	/**
	 * Moves the robot from Point A to Point B.
	 * 
	 * @param origin
	 * @param target
	 */
	public static void moveFromPointToPoint(Point origin, Point target) { 
		double x = target.x - origin.x;
		double y = target.y - origin.y;
		double[] d = cartesianToPolar(new Point(x, y));
		System.out.println("moveFromPointToPoint: New poloar coordinates: " + d[0] + " " + d[1]);
		double r = d[0];
		double phi = d[1] - robotRotation;
		//robotMove(phi, r, false);
		//robotDrive(distance_cm);
	}
}
