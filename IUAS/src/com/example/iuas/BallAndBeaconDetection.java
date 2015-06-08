
/**
 * This class holds all Beacons and Balls and decides if a object seen in the camera frame is a beacon or a ball.
 * It extends Listenable and implements a Thread Listener and Runnable.
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */


package com.example.iuas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import com.example.iuas.circle.Vector2;

import android.util.Pair;

public class BallAndBeaconDetection extends Listenable {

    public enum		 	 	OBJECT_TYPE {BALL, BEACON};
    public enum			 	COLOR {YELLOW, RED, GREEN, BLUE, ORANGE, WHITE};
    
    /*
     * lighter colors
     */
//    public final HashMap<COLOR, Scalar> COLOR_VALUE = new HashMap<COLOR, Scalar>(){{put(COLOR.YELLOW, new Scalar(144, 155, 48)); put(COLOR.RED, new Scalar (155, 44, 50));
//    put(COLOR.GREEN, new Scalar (33, 153, 109)); put(COLOR.BLUE, new Scalar(0, 182, 255)); put(COLOR.ORANGE, new Scalar(255,97,7));
//    put(COLOR.WHITE, new Scalar (255, 255, 255));}}; 
    
    /*
     * darker colors
     */
    public final HashMap<COLOR, Scalar> COLOR_VALUE = new HashMap<COLOR, Scalar>(){{put(COLOR.YELLOW, new Scalar(110, 86, 0)); put(COLOR.RED, new Scalar (89, 6, 0));
     							 	put(COLOR.GREEN, new Scalar (77, 127, 33)); put(COLOR.BLUE, new Scalar(1, 69, 84)); put(COLOR.ORANGE, new Scalar(255,97,7));
     							 	put(COLOR.WHITE, new Scalar (255, 255, 255));}}; 
     							 	
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

	private int 				 	ballCount = 0;
    private int						beaconCount = 0;
    private int						contoursCount = 0;
    private HashSet<BEACON>	 		currentBeacons = new HashSet<BEACON>();
    private HashMap<BEACON, Point> 	beaconImgCoords = new HashMap<BEACON, Point>();
    private Point					ballCoordinates;
	private static COLOR			BALL_COLOR;
    
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
        if(getBallCoordinates() != null){
        	lowestTargetPoint.add(new MatOfPoint(getBallCoordinates()));
        }
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
    	boolean[] isBeacon = new boolean[it.size()];
    	ArrayList<Pair<double[], COLOR>> blobs = new ArrayList<Pair<double[], COLOR>>();
    	
    	currentBeacons.clear();
    	contoursCount = it.size();
    	for(int i = 0; i < it.size(); i++ ){
    		blobs.add(new Pair<double[], COLOR>(calculateMatBounds(it.get(i).first), it.get(i).second));
    	}
    	
    	for(int i = 0; i < blobs.size(); i++ ){
    		for(int j = i+1; j < blobs.size(); j++){
	    		if(Utils.almostEqual(blobs.get(i).first[0], blobs.get(j).first[0], Utils.calculateEps(blobs.get(i).first[0], blobs.get(i).first[1], blobs.get(j).first[0], blobs.get(j).first[1], blobs.get(i).first[2], blobs.get(i).first[3], blobs.get(j).first[2], blobs.get(j).first[3]).x)){
    				if(Utils.almostEqual(blobs.get(i).first[1], blobs.get(j).first[1], Utils.calculateEps(blobs.get(i).first[0], blobs.get(i).first[1], blobs.get(j).first[0], blobs.get(j).first[1], blobs.get(i).first[2], blobs.get(i).first[3], blobs.get(j).first[2], blobs.get(j).first[3]).x)){
    					if(Utils.almostEqual(blobs.get(i).first[2], blobs.get(j).first[3], Utils.calculateEps(blobs.get(i).first[0], blobs.get(i).first[1], blobs.get(j).first[0], blobs.get(j).first[1], blobs.get(i).first[2], blobs.get(i).first[3], blobs.get(j).first[2], blobs.get(j).first[3]).y) && blobs.get(i).first[3] > blobs.get(j).first[3]) {
    						if(COLORS_BEACON.containsKey(new Pair<COLOR, COLOR>(it.get(j).second, it.get(i).second))) {
    							BEACON beacon = COLORS_BEACON.get(new Pair<COLOR, COLOR>(it.get(j).second, it.get(i).second));
    							currentBeacons.add(beacon);
    							beaconImgCoords.put(beacon, new Point((blobs.get(j).first[0] + blobs.get(j).first[1])/2, blobs.get(j).first[2]));
    							beaconCount = currentBeacons.size();
	    						isBeacon[i] = true;
	    						isBeacon[j] = true;
	    						break;
    						}
    					}
    					else if(Utils.almostEqual(blobs.get(j).first[2], blobs.get(i).first[3] , Utils.calculateEps(blobs.get(i).first[0], blobs.get(i).first[1], blobs.get(j).first[0], blobs.get(j).first[1], blobs.get(i).first[2], blobs.get(i).first[3], blobs.get(j).first[2], blobs.get(j).first[3]).y) && blobs.get(i).first[3] < blobs.get(j).first[3]) {
    						if(COLORS_BEACON.containsKey(new Pair<COLOR, COLOR>(it.get(i).second, it.get(j).second))) {
    							BEACON beacon = COLORS_BEACON.get(new Pair<COLOR, COLOR>(it.get(i).second, it.get(j).second));
    							currentBeacons.add(beacon);
    							beaconImgCoords.put(beacon, new Point((blobs.get(i).first[0] + blobs.get(i).first[1])/2, blobs.get(i).first[2]));
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
    	double yMin = Integer.MAX_VALUE;
    	Point temp = null;
    	for(int i = 0; i < isBeacon.length; i++){
    		if(!isBeacon[i]){
    			if(it.get(i).second == BALL_COLOR) {
    				ballCount++;
    				if(blobs.get(i).first[2] < yMin) {
    					temp = new Point((blobs.get(i).first[0]+blobs.get(i).first[1])/2, blobs.get(i).first[2]);
    				}
    			}
    			
    		}
    	}
    	if(temp != null) {
        	setBallCoordinates(Utils.convertImageToGround(new Vector2(temp.x, temp.y)));
        	informListeners(CatchBall.class);
        	
    	}
    	Utils.showLog("Contours count: " + contoursCount);
    	Utils.showLog("Ball count: " + ballCount);
    	Utils.showLog("Beacon count: " + beaconCount);
    	Utils.showLog("inform odometry");
    	informListeners(Odometry.class);
    }
    
    private double[] calculateMatBounds(MatOfPoint mat) {
    	double xMin = Integer.MAX_VALUE;
    	double xMax = 0;
    	double yMin = Integer.MAX_VALUE;
    	double yMax = 0;
    	for(Point p : mat.toArray()){
			xMin = p.x < xMin ? p.x : xMin;
			xMax = p.x > xMax ? p.x : xMax;
			yMin = p.y < yMin ? p.y : yMin;
			yMax = p.y > yMax ? p.y : yMax;
		}
    	return new double[] {xMin, xMax, yMin, yMax};
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
	
    public synchronized Point getBallCoordinates() {
		return ballCoordinates;
	}
	public synchronized void setBallCoordinates(Point ballCoordinates) {
		this.ballCoordinates = ballCoordinates;
	}
}
