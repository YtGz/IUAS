/**
 * This class provides a state machine for managing transitions from exploring the workspace to returning to the workspace after bringing the ball to the goal
 * It implements a Thread Listener and Runnable.
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import org.opencv.core.Point;


public class CatchBall implements ThreadListener, Runnable {
	
	private enum STATE {SEARCH_WORKSPACE, CATCH_BALL, BRING_BALL_TO_GOAL, RETURN_TO_ORIGIN};
	private volatile boolean ball;
	private STATE state;
	
	/**
	 * stars new thread a ball is seen
	 */
	public CatchBall() {
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * moving to the balls location and caging it with the bar
	 */
	public void catchBall(){
		//moveToPoint(currentBallPos);
		MainActivity.robotSetBar((byte) 255);
		
		
	}
	
	
	/**
	 * moving to the goal position and releasing the ball, while beeing sure not to hit the beacons
	 */
	public void bringBallToGoal(){
		//moveToPoint(goalPoint - 20cm x OR y)
		//turn until perpendicular to workspace edge
		//move 20 cm in x OR y direction
		MainActivity.robotSetBar((byte) 0);
	}
	
	/**
	 * returning from the goal position back into the workspace and moving to the origin
	 */
	public void returnToWorkspaceOrigin() {
		MainActivity.robotTurn(180);
		MainActivity.robotDrive(20);
		ball = false;
		//moveToPoint(0, 0);
		
	}
	
	/**
	 * searching the workspace:
	 * first turning around, then using the exploreWorkspace method
	 */
	public void searchWorkspace(){
		MainActivity.robotTurn(360);
		if(ball)
			return;
		exploreWorkspace();
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
	 * 
	 * @see local sheets where this path was drawn & calculated
	 */
	
	public void exploreWorkspace() {
		final int workspaceFactor = 1;
		final double density = Math.sqrt(17);  	//Math.sqrt(5); for 2 crossings / Math.sqrt(17); for 4 crossings / Math.sqrt(65); for 8 crossings
		MainActivity.robotTurn(-45);
		if(ball)
			return;
		MainActivity.robotDrive((int) (Math.sqrt(31250)/workspaceFactor));
		if(ball)
			return;
		MainActivity.robotTurn(135);
		if(ball)
			return;
		MainActivity.robotDrive((int) 300/workspaceFactor);
		if(ball)
			return;
		MainActivity.robotTurn((int)(Math.ceil((180 - (Math.toRadians(75/density))))));
		if(ball)
			return;
		MainActivity.robotDrive((int) (Math.sqrt(664062.5)/workspaceFactor));
		if(ball)
			return;
		MainActivity.robotTurn((int) (-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density))))))));
		if(ball)
			return;
		MainActivity.robotDrive((int) (Math.sqrt(664062.5)/workspaceFactor));
		if(ball)
			return;
		MainActivity.robotTurn((int) (Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density)))))));
		if(ball)
			return;
		MainActivity.robotDrive((int)(Math.sqrt(664062.5)/workspaceFactor));
		if(ball)
			return;
		MainActivity.robotTurn((int) (-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density))))))));
		if(ball)
			return;
		MainActivity.robotDrive((int) (Math.sqrt(664062.5)/workspaceFactor));
		if(ball)
			return;
		MainActivity.robotTurn((int)(Math.ceil((180 - (Math.toRadians(75/density))))));
		if(ball)
			return;
		MainActivity.robotDrive((int) (Math.sqrt(664062.5)/workspaceFactor));
		if(ball)
			return;
		MainActivity.robotTurn(135);
		if(ball)
			return;
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
	
	
	/** 
	 * Robot heads straight for the goal, ignoring obstacles, and in the end rotates according to theta.
	 * 
	 * @param x
	 * @param y
	 * @param theta
	 */
	public void navigateIgnoringObstacles(int x, int y, int theta) {
		int r = (int) Math.sqrt(x * x + y * y);
		int phi = (int) Math.toDegrees(Math.toRadians(90) - Math.atan2(y, x));
		//showLog(phi);
		phi *= -1;
		robotTurn(phi);
		robotDrive(r);
		robotTurn(theta - phi);
	}
	



	/* a ball was detected */
	@Override
	public void onEvent() {
		ball = true;
		Thread.interrupt(RobotControl.robotControlThread);
	}

}
	
	
	
	
	
	
	
