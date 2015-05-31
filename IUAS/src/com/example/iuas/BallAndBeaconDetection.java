package com.example.iuas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.util.Pair;

public class BallAndBeaconDetection extends Listenable implements ThreadListener, Runnable {

    public enum		 	 	OBJECT_TYPE {BALL, BEACON};
    public enum			 	COLOR {YELLOW, RED, GREEN, BLUE};
    
    /*
     * lighter colors
     */
//    public final HashMap<COLOR, Scalar> COLOR_VALUE = new HashMap<COLOR, Scalar>(){{put(COLOR.YELLOW, new Scalar(144, 155, 48)); put(COLOR.RED, new Scalar (155, 44, 50));
//    put(COLOR.GREEN, new Scalar (33, 153, 109)); put(COLOR.BLUE, new Scalar(0, 182, 255));}}; 
    
    /*
     * darker colors
     */
    public final HashMap<COLOR, Scalar> COLOR_VALUE = new HashMap<COLOR, Scalar>(){{put(COLOR.YELLOW, new Scalar(110, 86, 0)); put(COLOR.RED, new Scalar (89, 6, 0));
     							 	put(COLOR.GREEN, new Scalar (77, 127, 33)); put(COLOR.BLUE, new Scalar(1, 69, 84));}}; 
     							 	
    public enum			 	BEACON {YELLOW_RED, RED_YELLOW, BLUE_GREEN, RED_BLUE, BLUE_RED, BLUE_YELLOW, YELLOW_BLUE, RED_GREEN};
    
    public final HashMap<BEACON, Pair<COLOR, COLOR>> BEACON_COLORS = new HashMap<BEACON, Pair<COLOR, COLOR>>() {{put(BEACON.YELLOW_RED, new Pair(COLOR.YELLOW, COLOR.RED));
    							 	put(BEACON.RED_YELLOW, new Pair(COLOR.RED, COLOR.YELLOW)); put(BEACON.BLUE_RED, new Pair(COLOR.BLUE, COLOR.RED));
    							 	put(BEACON.RED_BLUE, new Pair(COLOR.RED, COLOR.BLUE)); put(BEACON.BLUE_YELLOW, new Pair(COLOR.BLUE, COLOR.YELLOW));
    							 	put(BEACON.YELLOW_BLUE, new Pair(COLOR.YELLOW, COLOR.BLUE)); put(BEACON.BLUE_GREEN, new Pair(COLOR.BLUE, COLOR.GREEN));
    							 	put(BEACON.RED_GREEN, new Pair(COLOR.RED, COLOR.GREEN));}};
    							 	
	public final HashMap<Pair<COLOR, COLOR>, BEACON> COLORS_BEACON = new HashMap<Pair<COLOR, COLOR>, BEACON>() {{
		 for(HashMap.Entry<BEACON, Pair<COLOR, COLOR>> entry : BEACON_COLORS.entrySet()){
		     put(entry.getValue(), entry.getKey());
		 }
	}};
	
	/*
	 * normal workspace
	 */
    //private final HashMap<BEACON, Point> BEACON_LOC = new HashMap<BEACON, Point>(){{put(BEACON.YELLOW_BLUE, new Point(-125, 125)); put(BEACON.RED_BLUE, new Point(0, 125)); put(BEACON.YELLOW_RED, new Point(125, 125)); put(BEACON.BLUE_GREEN, new Point(-125, 0)); put(BEACON.RED_GREEN, new Point(125, 0)); put(BEACON.BLUE_YELLOW, new Point(-125, -125)); put(BEACON.BLUE_RED, new Point(0, -125)); put(BEACON.RED_YELLOW, new Point(125, -125));}};
	
	/*
	 * small workspace
	 */
    public final HashMap<BEACON, Point> BEACON_LOC = new HashMap<BEACON, Point>(){{put(BEACON.YELLOW_BLUE, new Point(-64.5, 64.5)); put(BEACON.RED_BLUE, new Point(0, 64.5)); put(BEACON.YELLOW_RED, new Point(64.5, 64.5)); put(BEACON.BLUE_GREEN, new Point(-64.5, 0)); put(BEACON.RED_GREEN, new Point(64.5, 0)); put(BEACON.BLUE_YELLOW, new Point(-64.5, -64.5)); put(BEACON.BLUE_RED, new Point(0, -64.5)); put(BEACON.RED_YELLOW, new Point(64.5, -64.5));}};

	private int 				 		ballCount = 0;
    private int						beaconCount = 0;
    private int						contoursCount = 0;
    private HashSet<BEACON>	 		currentBeacons = new HashSet<BEACON>();
    private HashMap<BEACON, Point> 	beaconImgCoords = new HashMap<BEACON, Point>();
    private static COLOR					BALL_COLOR;
    
    protected final boolean DEBUG = true; // enables debug messages
	protected final int DEBUG_DEVICE = 1; // 1: sysout, 2: textLog.append --> atm no textLog defined here, so only sysout available
	protected final int USE_DEVICE = 1; // 1: USB, 2: Bluetooth
	private Thread t;
    
    /**
     * Detect balls and beacons.
     */
    public ArrayList<ArrayList<MatOfPoint>> detect(Mat inputFrameRgba, ColorBlobDetector mDetector) {
    	ballCount = 0;
    	contoursCount = 0;
    	beaconCount = 0;
    	Point a = new Point();
    	ArrayList<Pair<MatOfPoint, COLOR>>  contoursAccepted = new ArrayList<Pair<MatOfPoint, COLOR>>();
    	for(COLOR c : COLOR.values()){
    		//mDetector.setColorRadius(new Scalar(25,120,120,0));
    		mDetector.setHsvColor(Utils.converScalarRgb2Hsv(COLOR_VALUE.get(c)));
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
     * detect if object is a ball or a beacon and if it a beacon, which beacon
     * 
     * @param it
     */
    public void identifyObjects(ArrayList<Pair<MatOfPoint, COLOR>> it) {
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
	    		
    			if(Utils.almostEqual(xMin1, xMin2, Utils.calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).x)){
    				if(Utils.almostEqual(xMax1, xMax2, Utils.calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).x)){
    					if(Utils.almostEqual(yMin1, yMax2, Utils.calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).y) && yMax1 > yMax2) {
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
    					else if(Utils.almostEqual(yMin2, yMax1 , Utils.calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).y) && yMax1 < yMax2) {
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
    	informListeners();
    }
    
    public synchronized int getBallCount() {
		return ballCount;
	}

	public synchronized void setBallCount(int ballCount) {
		this.ballCount = ballCount;
	}

	public synchronized int getBeaconCount() {
		return beaconCount;
	}

	public synchronized void setBeaconCount(int beaconCount) {
		this.beaconCount = beaconCount;
	}

	public synchronized int getContoursCount() {
		return contoursCount;
	}

	public synchronized void setContoursCount(int contoursCount) {
		this.contoursCount = contoursCount;
	}

	public synchronized HashSet<BEACON> getCurrentBeacons() {
		return currentBeacons;
	}

	public synchronized void setCurrentBeacons(HashSet<BEACON> currentBeacons) {
		this.currentBeacons = currentBeacons;
	}

	public synchronized HashMap<BEACON, Point> getBeaconImgCoords() {
		return beaconImgCoords;
	}

	public synchronized void setBeaconImgCoords(HashMap<BEACON, Point> beaconImgCoords) {
		this.beaconImgCoords = beaconImgCoords;
	}

	public static synchronized COLOR getBALL_COLOR() {
		return BALL_COLOR;
	}

	public static synchronized void setBALL_COLOR(COLOR BALL_COLOR) {
		BallAndBeaconDetection.BALL_COLOR = BALL_COLOR;
	}

	@Override
	public void onEvent() {
		if(t == null | !t.isAlive()) {
			t = new Thread(this);
			t.start();
		}
	}

	@Override
	public void run() {
		detect(BallCatchingActivity.mRgba, BallCatchingActivity.mDetector);
	}
}
