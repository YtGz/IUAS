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
	private boolean ball;
	private STATE state;
	
	/**
	 * Starts new thread when object is created
	 */
	public CatchBall() {
		state = STATE.SEARCH_WORKSPACE;
		Thread t = new Thread(this);
		t.start();
	}
	
	/**
	 * moving to the balls location and caging it with the bar
	 */
	public void catchBall(){
		//moveToEgocentricPoint(currentBallPos);
		RobotControl.control("setBar", 0);	
	}
	
	
	/**
	 * moving to the goal position and releasing the ball, while beeing sure not to hit the beacons
	 */
	public void bringBallToGoal(){
		if(Math.abs(goalPoint.x)< 125 && Math.abs(goalPoint.y) < 125){
			moveToEgocentricPoint(new Point(goalPoint.sub(CameraFrameProcessingActivity.localization.getOdometryData().first)));
		}
		else{
			
			int rotation;
			if(goalPoint.x >= 125 ){
				goalPoint.x = goalPoint.x - 20;
				rotation = 0;
			}
			else if (goalPoint.x <= -125){
				goalPoint.x = goalPoint.x -20;
				rotation = 180;
			}
			else if(goalPoint.y >= 125){
				goalPoint.y = goalPoint.y -20;
				rotation = 90;
			}
			else if(goalPoint.y <= -125){
				goalPoint.y = goalPoint.y -20;
				rotation = 270;
			}
			moveToEgocentricPoint(new Point(goalPoint.sub(CameraFrameProcessingActivity.localization.getOdometryData().first)));
			RobotControl.control("turn", rotation - (int) Math.floor(CameraFrameProcessingActivity.localization.getOdometryData().second));
		}
		RobotControl.control("setBar", 255);
	}
	
	/**
	 * returning from the goal position back into the workspace and moving to the origin
	 */
	public void returnToWorkspaceOrigin() {
		RobotControl.control("turn", 180);
		RobotControl.control("drive", 20);
		setBall(false);
		moveToEgocentricPoint(new Point(0, 0));
		
	}
	
	/**
	 * searching the workspace:
	 * first turning around, then using the exploreWorkspace method
	 */
	public void searchWorkspace(){
		RobotControl.control("turn", 360);
		if(isBall())
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
		RobotControl.control("turn", -45);
		if(isBall())
			return;
		RobotControl.control("drive", (int) (Math.sqrt(31250)/workspaceFactor));
		if(isBall())
			return;
		RobotControl.control("turn", 135);
		if(isBall())
			return;
		RobotControl.control("drive", (int) 300/workspaceFactor);
		if(isBall())
			return;
		RobotControl.control("turn", (int)(Math.ceil((180 - (Math.toRadians(75/density))))));
		if(isBall())
			return;
		RobotControl.control("drive", (int) (Math.sqrt(664062.5)/workspaceFactor));
		if(isBall())
			return;
		RobotControl.control("turn", (int) (-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density))))))));
		if(isBall())
			return;
		RobotControl.control("drive", (int) (Math.sqrt(664062.5)/workspaceFactor));
		if(isBall())
			return;
		RobotControl.control("turn", (int) (Math.ceil(2*(90-(Math.asin(Math.toRadians(75/density)))))));
		if(isBall())
			return;
		RobotControl.control("drive", (int)(Math.sqrt(664062.5)/workspaceFactor));
		if(isBall())
			return;
		RobotControl.control("turn", (int) (-Math.ceil((2*(90-(Math.asin(Math.toRadians(75/density))))))));
		if(isBall())
			return;
		RobotControl.control("drive", (int) (Math.sqrt(664062.5)/workspaceFactor));
		if(isBall())
			return;
		RobotControl.control("turn", (int)(Math.ceil((180 - (Math.toRadians(75/density))))));
		if(isBall())
			return;
		RobotControl.control("drive", (int) (Math.sqrt(664062.5)/workspaceFactor));
		if(isBall())
			return;
		RobotControl.control("turn", 135);
		if(isBall())
			return;
		RobotControl.control("drive", (int) (Math.sqrt(31250)/workspaceFactor));
	}
	
	
	/** 
	 * Robot heads straight for the goal, ignoring obstacles.
	 * 
	 * @param x
	 * @param y
	 */
	public void moveToEgocentricPoint(Point p) {
		int r = (int) Math.sqrt(p.x * p.x + p.y * p.y);
		int phi = (int) Math.toDegrees(Math.toRadians(90) - Math.atan2(p.y, p.x));
		phi *= -1;
		RobotControl.control("turn", phi);
		if(isBall())
			return;
		RobotControl.control("drive", r);
	}
	
	private synchronized boolean isBall() {
		return ball;
	}

	private synchronized void setBall(boolean ball) {
		this.ball = ball;
	}

	/* a ball was detected */
	@Override
	public void onEvent() {
		if(isBall() != true) {
			setBall(true);
			if(RobotControl.robotControlThread != null)
				Thread.interrupt(RobotControl.robotControlThread);
		}
	}

}
	
	
	
	
	
	
	
