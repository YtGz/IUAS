/**
 * This class provides a state machine for managing transitions from exploring the workspace to returning to the workspace after bringing the ball to the goal
 * It implements a Thread Listener and Runnable.
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import com.example.iuas.circle.Vector2;


public class CatchBall implements ThreadListener, Runnable {
	
	private enum STATE {SEARCH_WORKSPACE, CATCH_BALL, BRING_BALL_TO_GOAL, RETURN_TO_ORIGIN};
	private boolean ball;
	private STATE state;

	private final Vector2 GOAL_POSITION = new Vector2(0, 0);
	
	/**
	 * Starts new thread when object is created
	 */
	public CatchBall() {
		state = STATE.SEARCH_WORKSPACE;
		Thread t = new Thread(this);
		t.start();
		Utils.showLog("DFSM started");
	}
	
	/**
	 * moving to the balls location and caging it with the bar
	 */
	public void catchBall(){
		//moveToEgocentricPoint(CameraFrameProcessingActivity.localization.getOdometryData().first);
		//RobotControl.control("setBar", 0);	
	}
	
	
	/**
	 * moving to the goal position and releasing the ball, while being sure not to hit the beacons
	 */
	public void bringBallToGoal(){
		if(Math.abs(GOAL_POSITION.x) < 125 && Math.abs(GOAL_POSITION.y) < 125){
			moveToEgocentricPoint(GOAL_POSITION.sub(CameraFrameProcessingActivity.localization.getOdometryData().first));
		}
		else{
			
			int rotation = 0;
			Vector2 goalPosition = new Vector2(GOAL_POSITION.x, GOAL_POSITION.y);
			if(GOAL_POSITION.x >= 125 ){
				goalPosition = new Vector2(GOAL_POSITION.x - 20, GOAL_POSITION.y);
				rotation = 0;
			}
			else if (GOAL_POSITION.x <= -125){
				goalPosition = new Vector2(GOAL_POSITION.x - 20, GOAL_POSITION.y);
				rotation = 180;
			}
			else if(GOAL_POSITION.y >= 125){
				goalPosition = new Vector2(GOAL_POSITION.x, GOAL_POSITION.y - 20);
				rotation = 90;
			}
			else if(GOAL_POSITION.y <= -125){
				goalPosition = new Vector2(GOAL_POSITION.x, GOAL_POSITION.y - 20);
				rotation = 270;
			}
			moveToEgocentricPoint(goalPosition.sub(CameraFrameProcessingActivity.localization.getOdometryData().first));
			RobotControl.control("turn", rotation - (int) Math.floor(CameraFrameProcessingActivity.localization.getOdometryData().second));
		}
		RobotControl.control("setBar", 255);
	}
	
	/**
	 * returning from the goal position back into the workspace and moving to the origin
	 */
	public void returnToWorkspaceOrigin() {
//		RobotControl.control("turn", 180);
//		RobotControl.control("drive", 20);
		setBall(false);
		moveToEgocentricPoint(Vector2.NULL);	
	}
	
	/**
	 * searching the workspace:
	 * first turning around, then using the exploreWorkspace method
	 */
	public void searchWorkspace(){
		if(isBall())
			return;
		RobotControl.control("turn", 360);
		if(isBall())
			return;
		//exploreWorkspace();
	}

	@Override
	public void run() {
		for(;;) {
			System.out.println("State: " + state);
			switch(getState()) {
			case SEARCH_WORKSPACE:
				searchWorkspace();
				break;
			case CATCH_BALL:
				catchBall();
				//setState(STATE.SEARCH_WORKSPACE);
				//setState(STATE.BRING_BALL_TO_GOAL);
				break;
			case BRING_BALL_TO_GOAL:
				bringBallToGoal();
				setState(STATE.RETURN_TO_ORIGIN);
				break;
			case RETURN_TO_ORIGIN:
				returnToWorkspaceOrigin();
				if(!isBall())
					setState(STATE.SEARCH_WORKSPACE);
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
		final double workspaceFactor = 4;
		final double density = Math.sqrt(17);  	//Math.sqrt(5); for 2 crossings / Math.sqrt(17); for 4 crossings / Math.sqrt(65); for 8 crossings
		if(isBall())
			return;
		RobotControl.control("turn", -45);
		if(isBall())
			return;
		RobotControl.control("drive", (int) (Math.sqrt(31250)/workspaceFactor));
		if(isBall())
			return;
		RobotControl.control("turn", 135);
		if(isBall())
			return;
		RobotControl.control("drive", (int) (300/workspaceFactor));
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
	public void moveToEgocentricPoint(Vector2 p) {
		int r = (int) Math.sqrt(p.x * p.x + p.y * p.y);
		int phi = (int) Math.toDegrees(Math.toRadians(90) - Math.atan2(p.y, p.x));
		phi *= -1;
		if(isBall())
			return;
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
	
	private synchronized STATE getState() {
		return state;
	}

	private synchronized void setState(STATE state) {
		this.state = state;
	}

	/* a ball was detected */
	@Override
	public void onEvent() {
		if(isBall() != true) {
			setState(STATE.CATCH_BALL);
			setBall(true);
			if(RobotControl.getRobotControlThread() != null)
				RobotControl.getRobotControlThread().interrupt();
		}
	}

}
	
	
	
	
	
	
	
