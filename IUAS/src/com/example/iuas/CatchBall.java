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
	
	/**
	 * Explores workspace ala "Zick-Zack".
	 * Makes one turn and one driving distance per method call.
	 * 
	 * @see local sheets where this path was drawn & calculated
	 */
	
	public void exploreWorkspace() {
		final int workspaceFactor = 1;
		final double density = Math.sqrt(17);  	//Math.sqrt(5); for 2 crossings / Math.sqrt(17); for 4 crossings / Math.sqrt(65); for 8 crossings
		MainActivity.robotTurn(-45);
		MainActivity.robotDrive((int) (Math.sqrt(31250)/workspaceFactor));
		MainActivity.robotTurn(135);
		MainActivity.robotDrive((int) 300/workspaceFactor);
		MainActivity.robotTurn((int)(Math.ceil((180 - (Math.toRadians(75/density))))));
		MainActivity.robotDrive((int) (Math.sqrt(664062.5)/workspaceFactor));
		MainActivity.robotTurn((int) (-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density))))))));
		MainActivity.robotDrive((int) (Math.sqrt(664062.5)/workspaceFactor));
		MainActivity.robotTurn((int) (Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density)))))));
		MainActivity.robotDrive((int)(Math.sqrt(664062.5)/workspaceFactor));
		MainActivity.robotTurn((int) (-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density))))))));
		MainActivity.robotDrive((int) (Math.sqrt(664062.5)/workspaceFactor));
		MainActivity.robotTurn((int)(Math.ceil((180 - (Math.toRadians(75/density))))));
		MainActivity.robotDrive((int) (Math.sqrt(664062.5)/workspaceFactor));
		MainActivity.robotTurn(135);
		MainActivity.robotDrive((int) (Math.sqrt(31250)/workspaceFactor));
		
//		if(robotMove(-45, Math.sqrt(45000)/workspaceFactor, true)) return;
//		if(robotMove(135, 300/workspaceFactor, true)) return;
//		if(robotMove(Math.ceil((180 - (Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
//		if(robotMove(-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density)))))), Math.sqrt(95625)/workspaceFactor, true)) return;
//		if(robotMove(Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
//		if(robotMove(-Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density))))), Math.sqrt(95625)/workspaceFactor, true)) return;
//		if(robotMove(Math.ceil(180 - (Math.asin(Math.toRadians(75/density)))), 300/workspaceFactor, true)) return;
//		robotMove(135, Math.sqrt(45000)/workspaceFactor, true);	
	}
	
}
}
	
	
	
	
	
	
	
