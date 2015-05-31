package com.example.iuas;

import org.opencv.core.Point;


public class CatchBall{
	Point target;
	Point origin;
	
	public void catchBall(int distance_cm){
		CopyOfBallCatchingActivity.moveFromPointToPoint(origin, target);
		MainActivity.robotSetBar((byte) 255);
		
		
	}
	
	public void bringBallToGoal(int distance_cm, int degree){
		MainActivity.robotTurn(degree);
		MainActivity.robotDrive(distance_cm);
		MainActivity.robotSetBar((byte) 0);
	}
	
	
	public void returnToWorkspace(Point origin, Point target){
		MainActivity.robotTurn(180);
		MainActivity.robotDrive(20);
		while(/*CopyOfBallCatchingActivity.moveFromPointToPoint(origin, target)*/true){
			//detectBall
				break;
		}
	}
	
	public void searchWorkspace(){
		MainActivity.robotTurn(360);
		CopyOfBallCatchingActivity.moveFromPointToPoint(origin, target);
		while(/*CopyOfBallCatchingActivity.exploreWorkspace()*/true){
			//detectBall
				break;
		}
	}
		
}
	
	
	
	
	
	
	
