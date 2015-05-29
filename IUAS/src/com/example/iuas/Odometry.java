package com.example.iuas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import com.example.iuas.BallAndBeaconDetection.BEACON;
import com.example.iuas.circle.Circle;
import com.example.iuas.circle.CircleCircleIntersection;
import com.example.iuas.circle.Vector2;

public class Odometry {
	public static Vector2 position;
	public static double rotation;
	
	public static void selfLocalize(HashSet<BEACON> beaconSet, HashMap<BEACON, Point> beaconImgCoords) {
		if(beaconSet.size() > 1) {
			BEACON[] beacons = (BEACON[]) beaconSet.toArray();
			Vector2[] beaconCoordinatesImage = new Vector2[2];
			Vector2[] beaconCoordinatesEgocentric = new Vector2[2];
			Vector2[] beaconCoordinatesWorld = new Vector2[2];
			double[] beaconDistance = new double[2];
			for(int i = 0; i < 2; i++) {
				beaconCoordinatesImage[i] = new Vector2(beaconImgCoords.get(beacons[i]).x, beaconImgCoords.get(beacons[i]).y);
				beaconCoordinatesEgocentric[i] = new Vector2(convertImageToGround(beaconCoordinatesImage[i]).x, convertImageToGround(beaconCoordinatesImage[i]).y);
				beaconCoordinatesWorld[i] = new Vector2(BallAndBeaconDetection.BEACON_LOC.get(beacons[i]).x, BallAndBeaconDetection.BEACON_LOC.get(beacons[i]).y);
				beaconDistance[i] = Math.sqrt((Math.pow(beaconCoordinatesEgocentric[i].x, 2) + Math.pow(beaconCoordinatesEgocentric[i].y, 2)));
			}
			Circle[] circles = {new Circle(beaconCoordinatesWorld[0], beaconDistance[0]), new Circle(beaconCoordinatesWorld[1], beaconDistance[1])};
			CircleCircleIntersection cci = new CircleCircleIntersection(circles[0], circles[1]);
			Vector2[] intersectionPoints = cci.getIntersectionPoints();
			if(intersectionPoints.length < 1) {
				System.out.println("FATAL ERROR: No intersection points.");
			}
			else if (intersectionPoints.length < 2) {
				position = intersectionPoints[0];
			}
			else {
				int leftBeacon = 0; //beacon to the left from the robot(!)
				int rightBeacon = 1; //beacon to the right from the robot(!)
				if(beaconCoordinatesEgocentric[0].x > beaconCoordinatesEgocentric[1].x) {
					leftBeacon = 1;
					rightBeacon = 0;
				}
				if(beaconCoordinatesEgocentric[leftBeacon].x != beaconCoordinatesEgocentric[rightBeacon].x) {
					if(beaconCoordinatesEgocentric[leftBeacon].x < beaconCoordinatesEgocentric[rightBeacon].x) {
						if(intersectionPoints[0].y < intersectionPoints[1].y) {		//upper point
							position = intersectionPoints[0];
						}
						else {
							position = intersectionPoints[1];
						}
					}
					else
						if(intersectionPoints[0].y > intersectionPoints[1].y) {		//lower point
							position = intersectionPoints[0];
						}
						else {
							position = intersectionPoints[1];
						}
				}
				else {
					if(beaconCoordinatesEgocentric[leftBeacon].y > beaconCoordinatesEgocentric[rightBeacon].y) {
						if(intersectionPoints[0].x < intersectionPoints[1].x) {		//left point
							position = intersectionPoints[0];
						}
						else {
							position = intersectionPoints[1];
						}
					}
					else {
						if(intersectionPoints[0].x > intersectionPoints[1].x) {		//right point
							position = intersectionPoints[0];
						}
						else {
							position = intersectionPoints[1];
						}
					}
				}
			}
			//calculate angle
		}
		else {
			System.out.println("Only found " + beaconSet.size() + " beacons. No localization possible.");
		}
	}
	
    public static Point convertImageToGround(Vector2 p){
    	Mat src =  new Mat(1, 1, CvType.CV_32FC2);
        Mat dest = new Mat(1, 1, CvType.CV_32FC2);
        src.put(0, 0, new double[] { p.x, p.y }); // p is a point in image coordinates
        Core.perspectiveTransform(src, dest, ColorBlobDetectionActivity.homography); //homography is the homography matrix
        Point dest_point = new Point(dest.get(0, 0)[0], dest.get(0, 0)[1]);
       // showLog("convertImageToGround: dest_point: " + dest_point);
        return dest_point;
    }
}
