package com.example.iuas;


public class DetectObstacles extends Listenable implements Runnable{

	@Override
	public void run() {
		if(SensorData.detectObstacle(new boolean[] { true, true, true })){
			informListeners(CatchBall.class);
		}
		
		
	}
	
}
