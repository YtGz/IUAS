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

	/* a ball was detected */
	@Override
	public void onEvent() {
		ball = true;
		Thread.interrupt(RobotControl.robotControlThread);
	}
}
	
	
	
	
	
	
	
