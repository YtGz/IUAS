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

public class BallCatchingActivity extends MainActivity implements CvCameraViewListener2, Runnable {
    private Mat                  mRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Scalar               CONTOUR_COLOR;
    private Scalar				 POINT_COLOR;
    private Mat				 	 homography;
    private Point				 lowestTargetPoint;
    private Point				 robotPosition;
    private double				 robotRotation;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                	System.out.println("OpenCV loaded successfully");
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
        System.out.println("Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("called onCreate");
        super.onBallCatchingActivityCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.ball_catching_view);
        
        textLog = (TextView) findViewById(R.id.textLog);
        com = new FTDriver((UsbManager) getSystemService(USB_SERVICE));
		connect();
        
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
    	System.out.println("Cam View: " + mOpenCvCameraView);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mDetector.setHsvColor(mBlobColorHsv);
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
	   // System.out.println("Contours count: " + contours.size());
	    if(contours.size() == 1) {
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
    
    
    public void backButtonOnClick(View view) {
        finish();
    }
    
    public void catchBallOnClick(View view){
    	Thread t = new Thread(this);
    	t.start();
    }
    
    public Point convertImageToGround(Point p){
    	Mat src =  new Mat(1, 1, CvType.CV_32FC2);
        Mat dest = new Mat(1, 1, CvType.CV_32FC2);
        src.put(0, 0, new double[] { p.x, p.y }); // p is a point in image coordinates
        Core.perspectiveTransform(src, dest, homography); //homography is the homography matrix
        Point dest_point = new Point(dest.get(0, 0)[0], dest.get(0, 0)[1]);
       // System.out.println(dest_point);
        return dest_point;
    }
    
    public void work() {
    	ArrayList<Double> l = new ArrayList<Double>();
    	for(int i = 0; i < 50000; i++) {
    		l.add(Math.random());
    	}
    	Collections.sort(l);
    	System.out.println("Finished wait");
    }

	@Override
	public void run() {
		catchBall(75, 75);
	}
	
	public void catchBall(double x, double y){
		if(!turnToDetectObstacle()) {
			exploreWorkspace();
		}
		//deliver ball to target position
		moveFromPointToPoint(robotPosition, new Point(x, y));
		robotSetBar((byte) 255);
		//return to origin
		moveFromPointToPoint(robotPosition, new Point(0, 0));
		robotMove(-robotRotation, 0, false);
	}
	
	public boolean turnToDetectObstacle(){
		if(robotMove(0, 0, true)) {
			return true;
		}
		for (int i = 1; i <= 360; i += DELTA_R) {
			if(robotMove(DELTA_R, 0, true)) {
				return true;
			}
    	}
		return false;
	}

	private void detectedBall() {
		//work();
		Point p = convertImageToGround(lowestTargetPoint);
		System.out.println("lowest target point: "+p);
		p.x /= 10;
		p.y = p.y/10-20;
		double[] d = cartesianToPolar(p);
		System.out.println("polar to target point: " + d[1] + " " + d[0]);
		robotMove(d[1], d[0], false);
		robotMove(0, -5, false);
		robotSetBar((byte) 0);
		robotFlashLed(0);
	}
	
	/**
	 * Converts coordinates from polar to cartesian.
	 */
	public Point polarToCartesian(double phi, double r) {
		double xf = Math.cos(Math.toRadians(phi)) * r;
		double yf = Math.sin(Math.toRadians(phi)) * r;
		
		return new Point(xf, yf);
	}
	
	public double[] cartesianToPolar(Point p){
		double r =  Math.sqrt(p.x*p.x + p.y * p.y);
    	double phi =  Math.toDegrees(Math.atan2(p.y, p.x));
    	return new double[]{r, phi};
	}
	
	/**
	 * Lets the robot turn and move with the given parameters.
	 * Also returns a log of the robot's current position.
	 * 
	 * @param phi
	 * @param r
	 */
	public boolean robotMove(double phi, double r, boolean searchForBall) {
		int phiC = (int) Math.round(phi);
		int rC = (int) Math.round(r);
		if(phiC != 0) {
			robotTurn(phiC);
		}
		if(rC != 0) {
			robotDrive(rC);
		}
		//System.out.println("robotRot:" + robotRotation);
		robotRotation = phiC + robotRotation;
		System.out.println("phiC: " + phiC);
		System.out.println("robotRotstion: " + robotRotation);
		Point p = polarToCartesian(robotRotation, rC);
		robotPosition.x += p.x;
		robotPosition.y += p.y;
		
		System.out.println("Updated robot position: "+robotPosition);
		System.out.println("Updated rotation rel. to start: "+robotRotation);
		
		if (lowestTargetPoint != null && searchForBall) {
			detectedBall();
			return true;
		}
		return false;
	}
	
	/**
	 * Explores workspace ala "Zick-Zack".
	 * Makes one turn and one driving distance per method call.
	 */
	public void exploreWorkspace() {
		final int workspaceFactor = 2;
		final double density = Math.sqrt(17);  	//Math.sqrt(5); for 2 crossings / Math.sqrt(17); for 4 crossings / Math.sqrt(65); for 8 crossings
		if(robotMove(-45, Math.sqrt(45000)/workspaceFactor, true)) return;
		if(robotMove(135, 300/workspaceFactor, true)) return;
		if(robotMove(Math.ceil((180 - (Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
		if(robotMove(-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density)))))), Math.sqrt(95625)/workspaceFactor, true)) return;
		if(robotMove(Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
		if(robotMove(-Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
		if(robotMove(Math.ceil(180 - (Math.asin(Math.toRadians(75/density)))), 300/workspaceFactor, true)) return;
		robotMove(135, Math.sqrt(45000)/workspaceFactor, true);	
	}
	
	public void moveFromPointToPoint(Point origin, Point target) {
		double x = target.x - origin.x;
		double y = target.y - origin.y;
		double[] d = cartesianToPolar(new Point(x, y));
		System.out.println(d[0] + " " + d[1]);
		double r = d[0];
		double phi = d[1] - robotRotation;
		robotMove(phi, r, false);
	}
}
