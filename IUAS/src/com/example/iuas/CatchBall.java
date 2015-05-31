package com.example.iuas;

import org.opencv.core.Point;


public class CatchBall implements ThreadListener, Runnable {
	
	private enum STATE {SEARCH_WORKSPACE, CATCH_BALL, BRING_BALL_TO_GOAL, RETURN_TO_ORIGIN};
	private volatile boolean ball;
	private STATE state;
	
	public CatchBall() {
		Thread t = new Thread(this);
		t.start();
	}
	
	public void catchBall(){
		//moveToPoint(currentBallPos);
		MainActivity.robotSetBar((byte) 255);
		
		
	}
	
	public void bringBallToGoal(){
		//moveToPoint(goalPoint - 20cm x OR y)
		//turn until perpendicular to workspace edge
		//move 20 cm in x OR y direction
		MainActivity.robotSetBar((byte) 0);
	}
	
	
	public void returnToWorkspaceOrigin() {
		MainActivity.robotTurn(180);
		MainActivity.robotDrive(20);
		ball = false;
		//moveToPoint(0, 0);
		
	}
	
	public void searchWorkspace(){
		MainActivity.robotTurn(360);
		if(ball)
			return;
		//exploreWorkspace();
	}

	@Override
	public void run() {
		for(;;) {
			switch(state) {
			case SEARCH_WORKSPACE:
				searchWorkspace();
				break;
			case CATCH_BALL:
				catchBall();
				break;
			case BRING_BALL_TO_GOAL:
				bringBallToGoal();
				break;
			case RETURN_TO_ORIGIN:
				returnToWorkspaceOrigin();
				break;
			}
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
	



	/* a ball was detected */
	@Override
	public void onEvent() {
		ball = true;
		Thread.interrupt(RobotControl.robotControlThread);
	}

}
	
	
	
	
	
	
	
