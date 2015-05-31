/**
 * This class provides the beacon pairs to check if the combination the camera sees is possible or not
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import com.example.iuas.BallAndBeaconDetection.BEACON;

public class BeaconOrder {
	private static final BEACON[][] beaconPairsOrdered = {{BEACON.RED_GREEN, BEACON.RED_YELLOW}, {BEACON.RED_YELLOW, BEACON.BLUE_RED}, {BEACON.BLUE_RED, BEACON.BLUE_YELLOW}, {BEACON.BLUE_YELLOW, BEACON.BLUE_GREEN}, {BEACON.BLUE_GREEN, BEACON.YELLOW_BLUE}, {BEACON.YELLOW_BLUE, BEACON.RED_BLUE}, {BEACON.RED_BLUE, BEACON.YELLOW_RED}, {BEACON.YELLOW_RED, BEACON.RED_GREEN}}; 
	
	//returns 2 beacons that are neighbored, ordered clockwise
	public static BEACON[] getTwoNeighboredBeacons(ArrayList<BEACON> beacons) {
		for(int i = 0; i < beacons.size(); i++) {
			for(int j = i; j < beacons.size(); j++) {
				for(int k = 0; k < beaconPairsOrdered.length; k++) {
					if(beaconPairsOrdered[k][0].equals(beacons.get(i)) && beaconPairsOrdered[k][1].equals(beacons.get(j))) {
						return new BEACON[] {beacons.get(i), beacons.get(j)};
					}
					if(beaconPairsOrdered[k][1].equals(beacons.get(i)) && beaconPairsOrdered[k][0].equals(beacons.get(j))) {
						return new BEACON[] {beacons.get(j), beacons.get(i)};
					}
				}
			}
		}
		return null;
	}
}
