package com.example.iuas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class BallAndBeaconDetection {

    public static enum		 	 	OBJECT_TYPE {BALL, BEACON};
    public static enum			 	COLOR {YELLOW, RED, GREEN, BLUE};
    
    
    /*
     * lighter colors
     */
//    public static final HashMap<COLOR, Scalar> COLOR_VALUE = new HashMap<COLOR, Scalar>(){{put(COLOR.YELLOW, new Scalar(144, 155, 48)); put(COLOR.RED, new Scalar (155, 44, 50));
//    put(COLOR.GREEN, new Scalar (33, 153, 109)); put(COLOR.BLUE, new Scalar(0, 182, 255));}}; 
    
    /*
     * darker colors
     */
    public static final HashMap<COLOR, Scalar> COLOR_VALUE = new HashMap<COLOR, Scalar>(){{put(COLOR.YELLOW, new Scalar(110, 86, 0)); put(COLOR.RED, new Scalar (89, 6, 0));
     							 	put(COLOR.GREEN, new Scalar (77, 127, 33)); put(COLOR.BLUE, new Scalar(1, 69, 84));}}; 
    public static enum			 	BEACON {YELLOW_RED, RED_YELLOW, BLUE_GREEN, RED_BLUE, BLUE_RED, BLUE_YELLOW, YELLOW_BLUE, RED_GREEN};
    public static final HashMap<BEACON, Pair<COLOR, COLOR>> BEACON_COLORS = new HashMap<BEACON, Pair<COLOR, COLOR>>() {{put(BEACON.YELLOW_RED, new Pair(COLOR.YELLOW, COLOR.RED));
    							 	put(BEACON.RED_YELLOW, new Pair(COLOR.RED, COLOR.YELLOW)); put(BEACON.BLUE_RED, new Pair(COLOR.BLUE, COLOR.RED));
    							 	put(BEACON.RED_BLUE, new Pair(COLOR.RED, COLOR.BLUE)); put(BEACON.BLUE_YELLOW, new Pair(COLOR.BLUE, COLOR.YELLOW));
    							 	put(BEACON.YELLOW_BLUE, new Pair(COLOR.YELLOW, COLOR.BLUE)); put(BEACON.BLUE_GREEN, new Pair(COLOR.BLUE, COLOR.GREEN));
    							 	put(BEACON.RED_GREEN, new Pair(COLOR.RED, COLOR.GREEN));}};
	public static final HashMap<Pair<COLOR, COLOR>, BEACON> COLORS_BEACON = new HashMap<Pair<COLOR, COLOR>, BEACON>() {{
		 for(HashMap.Entry<BEACON, Pair<COLOR, COLOR>> entry : BEACON_COLORS.entrySet()){
		     put(entry.getValue(), entry.getKey());
		 }
	}};
    //private final HashMap<BEACON, Point> BEACON_LOC = new HashMap<BEACON, Point>(){{put(BEACON.YELLOW_BLUE, new Point(-125, 125)); put(BEACON.RED_BLUE, new Point(0, 125)); put(BEACON.YELLOW_RED, new Point(125, 125)); put(BEACON.BLUE_GREEN, new Point(-125, 0)); put(BEACON.RED_GREEN, new Point(125, 0)); put(BEACON.BLUE_YELLOW, new Point(-125, -125)); put(BEACON.BLUE_RED, new Point(0, -125)); put(BEACON.RED_YELLOW, new Point(125, -125));}};
    public static final HashMap<BEACON, Point> BEACON_LOC = new HashMap<BEACON, Point>(){{put(BEACON.YELLOW_BLUE, new Point(-64.5, 64.5)); put(BEACON.RED_BLUE, new Point(0, 64.5)); put(BEACON.YELLOW_RED, new Point(64.5, 64.5)); put(BEACON.BLUE_GREEN, new Point(-64.5, 0)); put(BEACON.RED_GREEN, new Point(64.5, 0)); put(BEACON.BLUE_YELLOW, new Point(-64.5, -64.5)); put(BEACON.BLUE_RED, new Point(0, -64.5)); put(BEACON.RED_YELLOW, new Point(64.5, -64.5));}};
    public static int 				 	ballCount = 0;
    public static int					beaconCount = 0;
    public static int					contoursCount = 0;
    public static HashSet<BEACON>	 	currentBeacons = new HashSet<BEACON>();
    public static HashMap<BEACON, Point> beaconImgCoords = new HashMap<BEACON, Point>();
    public static COLOR					BALL_COLOR;
    
    protected final boolean DEBUG = true; // enables debug messages
	protected final int DEBUG_DEVICE = 1; // 1: sysout, 2: textLog.append --> atm no textLog defined here, so only sysout available
	protected final int USE_DEVICE = 1; // 1: USB, 2: Bluetooth
	
    private static final String  TAG              = "OCVSample::Activity";
    
    /**
     * Detect balls and beacons.
     */
    public static ArrayList<ArrayList<MatOfPoint>> detect(Mat inputFrameRgba, ColorBlobDetector mDetector) {
    	ballCount = 0;
    	contoursCount = 0;
    	beaconCount = 0;
    	Point a = new Point();
    	ArrayList<Pair<MatOfPoint, COLOR>>  contoursAccepted = new ArrayList<Pair<MatOfPoint, COLOR>>();
    	for(COLOR c : COLOR.values()){
    		//mDetector.setColorRadius(new Scalar(25,120,120,0));
    		mDetector.setHsvColor(converScalarRgb2Hsv(COLOR_VALUE.get(c)));
            mDetector.process(inputFrameRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            // Count the amount of detected Pixels
        	int nPixel = 0;
        	
        	for(int i = 0; i < contours.size(); i++){
        		nPixel = contours.get(i).toArray().length;
	        	if(nPixel >= /*contoursCountThreshold*/0) {
	        		contoursAccepted.add(new Pair<MatOfPoint, COLOR>(contours.get(i), c));
	        	}
        	}
    	}
    	identifyObjects(contoursAccepted);
        ArrayList<MatOfPoint> lowestTargetPoint = new ArrayList<MatOfPoint>();
        lowestTargetPoint.add(new MatOfPoint(a));
        ArrayList<MatOfPoint> contoursOnly = new ArrayList<MatOfPoint>();
        for(Pair<MatOfPoint, COLOR> p : contoursAccepted) {
        	contoursOnly.add((MatOfPoint) p.first);
        }
        ArrayList<ArrayList<MatOfPoint>> r = new ArrayList<ArrayList<MatOfPoint>>();
        r.add(contoursOnly);
        r.add(lowestTargetPoint);
        return r;
    }
    
    /**
     * Converts scalar HSV values to RGB values.
     * 
     * @param hsvColor
     * @return
     */
    private static Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 3);

        return new Scalar(pointMatRgba.get(0, 0));
    }
    private static Scalar converScalarRgb2Hsv(Scalar rgbColor) {
        Mat pointMatHsv = new Mat();
        Mat pointMatRgba = new Mat(1, 1, CvType.CV_8UC3, rgbColor);
        Imgproc.cvtColor(pointMatRgba, pointMatHsv, Imgproc.COLOR_RGB2HSV_FULL, 3);

        return new Scalar(pointMatHsv.get(0, 0));
    }
    
    public static boolean almostEqual(double a, double b, double eps){
        return Math.abs(a-b)<eps;
    }
    
    public static Point calculateEps (double xMin1, double xMax1, double xMin2, double xMax2, double yMin1, double yMax1, double yMin2, double yMax2){
    	return new Point((((xMax1 - xMin1) + (xMax2 - xMin2))/2)*2, (((yMax1 - yMin1)+ (yMax2 - yMin2))/2)*.5);
    }
    
    public static OBJECT_TYPE[] identifyObjects(ArrayList<Pair<MatOfPoint, COLOR>> it) {
    	double xMin1 = Integer.MAX_VALUE;
    	double xMin2 = Integer.MAX_VALUE;
    	double xMax1 = 0;
    	double xMax2 = 0;
    	double yMin1 = Integer.MAX_VALUE;
    	double yMin2 = Integer.MAX_VALUE;
    	double yMax1 = 0;
    	double yMax2 = 0;
    	boolean[] isBeacon = new boolean[it.size()];
    	
    	currentBeacons.clear();
    	contoursCount = it.size();   	
    	
    	for(int i = 0; i < it.size(); i++ ){
    		xMin1 = Integer.MAX_VALUE;
        	xMax1 = 0;
        	yMin1 = Integer.MAX_VALUE;
        	yMax1 = 0;
    		for(Point p : it.get(i).first.toArray()){
    			xMin1 = p.x < xMin1 ? p.x : xMin1;
    			xMax1 = p.x > xMax1 ? p.x : xMax1;
    			yMin1 = p.y < yMin1 ? p.y : yMin1;
    			yMax1 = p.y > yMax1 ? p.y : yMax1;
    		}
    		for(int j = i+1; j < it.size(); j++){
    			xMin2 = Integer.MAX_VALUE;
            	xMax2 = 0;
            	yMin2 = Integer.MAX_VALUE;
            	yMax2 = 0;
    			for(Point p : it.get(j).first.toArray()){
        			xMin2 = p.x < xMin2 ? p.x : xMin2;
        			xMax2 = p.x > xMax2 ? p.x : xMax2;
        			yMin2 = p.y < yMin2 ? p.y : yMin2;
        			yMax2 = p.y > yMax2 ? p.y : yMax2;
        		}
	    		
    			if(almostEqual(xMin1, xMin2, calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).x)){
    				if(almostEqual(xMax1, xMax2 ,calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).x)){
    					if(almostEqual(yMin1, yMax2, calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).y) && yMax1 > yMax2) {
    						if(COLORS_BEACON.containsKey(new Pair<COLOR, COLOR>(it.get(j).second, it.get(i).second))) {
    							BEACON beacon = COLORS_BEACON.get(new Pair<COLOR, COLOR>(it.get(j).second, it.get(i).second));
    							currentBeacons.add(beacon);
    							beaconImgCoords.put(beacon, new Point((xMin2 + xMax2)/2, yMin2));
    							beaconCount = currentBeacons.size();
	    						isBeacon[i] = true;
	    						isBeacon[j] = true;
	    						break;
    						}
    					}
    					else if(almostEqual(yMin2, yMax1 , calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).y) && yMax1 < yMax2) {
    						if(COLORS_BEACON.containsKey(new Pair<COLOR, COLOR>(it.get(i).second, it.get(j).second))) {
    							BEACON beacon = COLORS_BEACON.get(new Pair<COLOR, COLOR>(it.get(i).second, it.get(j).second));
    							currentBeacons.add(beacon);
    							beaconImgCoords.put(beacon, new Point((xMin1 + xMax1)/2, yMin1));
	    						beaconCount = currentBeacons.size();
	    						isBeacon[i] = true;
	    						isBeacon[j] = true;
	    						break;
    						}
    					}
    				}
    			}
    			
    		}
    	}
    	
    	for(int i = 0; i < isBeacon.length; i++){
    		if(!isBeacon[i]){
    			if(it.get(i).second == BALL_COLOR) {
    				ballCount++;
        			//choose active ball
        			//save ball coordinates
    			}
    		}
    	}
    	System.out.println("Contours count: " + contoursCount);
    	System.out.println("Ball count: " + ballCount);
    	System.out.println("Beacon count: " + beaconCount);
		return null;
    }
}
