/**
 * This class handles the self localization of the robot with the beacons.
 * It implements a Thread Listener and Runnable.
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */


package com.example.iuas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.opencv.core.Point;

import android.util.Pair;

import com.example.iuas.BallAndBeaconDetection.BEACON;
import com.example.iuas.circle.Circle;
import com.example.iuas.circle.CircleCircleIntersection;
import com.example.iuas.circle.Vector2;

public class Odometry implements ThreadListener, Runnable {
	private Pair<Vector2, Double> odometryData = new Pair<Vector2, Double>(Vector2.NULL, 0.0);
	private Thread t;

	
	/**
	 *calculating the robot position with beacons 
	 *
	 * @param beaconSet
	 * @param beaconImgCoords
	 */
	public void selfLocalize(HashSet<BEACON> beaconSet, HashMap<BEACON, Point> beaconImgCoords) {
		if(beaconSet.size() > 1) {
			ArrayList<BEACON> beacons = new ArrayList <BEACON>();
			for(BEACON beacon : beaconSet){
				beacons.add(beacon);
			}
			BEACON[] beaconPair = BeaconOrder.getTwoNeighboredBeacons(beacons);
			if(beaconPair != null) {
				Vector2[] beaconCoordinatesImage = new Vector2[2];
				Vector2[] beaconCoordinatesEgocentric = new Vector2[2];
				Vector2[] beaconCoordinatesWorld = new Vector2[2];
				double[] beaconDistance = new double[2];
				for(int i = 0; i < 2; i++) {
					beaconCoordinatesImage[i] = new Vector2(beaconImgCoords.get(beaconPair[i]).x, beaconImgCoords.get(beaconPair[i]).y);
					beaconCoordinatesEgocentric[i] = new Vector2(Utils.convertImageToGround(beaconCoordinatesImage[i]).x/10, Utils.convertImageToGround(beaconCoordinatesImage[i]).y/10);
					beaconCoordinatesWorld[i] = new Vector2(CameraFrameProcessingActivity.ballDetection.BEACON_LOC.get(beaconPair[i]).x, CameraFrameProcessingActivity.ballDetection.BEACON_LOC.get(beaconPair[i]).y);
					beaconDistance[i] = Math.sqrt((Math.pow(beaconCoordinatesEgocentric[i].x, 2) + Math.pow(beaconCoordinatesEgocentric[i].y, 2)));
				}
				Utils.showLog("Debug: beacons: " + beaconPair[0] + " | " + beaconPair[1]);
				Utils.showLog("Debug: circleOnepos:" + beaconCoordinatesWorld[0]);
				Utils.showLog("Debug: beaconDistance " + beaconDistance[0]);
				Utils.showLog("Debug: circleTwopos:" + beaconCoordinatesWorld[1]);
				Utils.showLog("Debug: beaconTwoDistance " + beaconDistance[1]);
				Circle[] circles = {new Circle(beaconCoordinatesWorld[0], beaconDistance[0]), new Circle(beaconCoordinatesWorld[1], beaconDistance[1])};
				CircleCircleIntersection cci = new CircleCircleIntersection(circles[0], circles[1]);
				Vector2[] intersectionPoints = cci.getIntersectionPoints();
				Vector2 p;
				if(intersectionPoints.length < 1) {
					Utils.showLog("FATAL ERROR: No intersection points.");
					return;
				}
				else if (intersectionPoints.length < 2) {
					Utils.showLog("Only one intersection point!");
					p = intersectionPoints[0];
				}
				else {
					Utils.showLog("Two intersection points");
					//Determine which intersection point to use
					int near;
					int far;
					if(intersectionPoints[0].mod() < intersectionPoints[1].mod()) {
						near = 0;
						far = 1;
					}
					else {
						near = 1;
						far = 0;
					}
					if(beaconCoordinatesEgocentric[0].x > beaconCoordinatesEgocentric[1].x) {	// Robot outside of test area
						p = intersectionPoints[far];
					}
					else {
						p = intersectionPoints[near];
					}
				}
				//calculate angle
				Vector2 robotToLeftBeacon = beaconCoordinatesWorld[0].sub(p);
				double r = Math.toDegrees(Math.acos(robotToLeftBeacon.dot(Vector2.X)/(robotToLeftBeacon.mod()* Vector2.X.mod())));
				double phi =  Math.toDegrees(Math.atan2(beaconCoordinatesEgocentric[0].y, beaconCoordinatesEgocentric[0].x))-90;
				r = r - phi;
				//setOdometryData(new Pair<Vector2, Double>(p, r));
				System.out.println("Current odometry:   x: " + p.x + "  y: " + p.y + "  theta: " + r);
			}
		}
		else {
			System.out.println("Only found " + beaconSet.size() + " beacons. No localization possible.");
		}
	}
    
	public synchronized Pair<Vector2, Double> getOdometryData() {
		return odometryData;
	}

	public synchronized void setOdometryData(Pair<Vector2, Double> odometryData) {
		this.odometryData = odometryData;
	}

	@Override
	public void onEvent() {
		Utils.showLog("start odometry");
		if(t == null || !t.isAlive()) {
			t = new Thread(this);
			t.start();
		}
	}

	@Override
	public void run() {
		selfLocalize(CameraFrameProcessingActivity.ballDetection.getCurrentBeacons(), CameraFrameProcessingActivity.ballDetection.getBeaconImgCoords());	
	}
}
