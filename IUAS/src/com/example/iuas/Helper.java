package com.example.iuas;

public class Helper {

	public static double remap(double oldLowerBound, double oldUpperBound, double newLowerBound, double newUpperBound, double value) {
		if (oldLowerBound != oldUpperBound && newLowerBound != newUpperBound)
            return (((value - oldLowerBound) * (newUpperBound - newLowerBound)) / (oldUpperBound - oldLowerBound)) + newLowerBound;
        else
            return (newUpperBound + newLowerBound) / 2;
	}
}
