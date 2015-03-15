package com.example.iuas.simulations;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;


class Surface extends JPanel {
	
	//The length of an arch of the lemniscate. 
	private double a = 200;
	
	/* The smaller this value the more frequently the robot repeats it's move & turn cycles along the path.
	 * A small value leads to more accuracy but the robot will get slower.
	 * Number of move & turn cycles: 2*pi/resolution.
	 */
    private double resolution = .1;
    
    
    
    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        
        //These values need to be the center of the window.
        double oldX = 400;
        double oldY = 400;
        for(double t = 0; t < 2*Math.PI; t+=resolution) {
        	double newX = a*Math.sqrt(2)*Math.cos(t)/(Math.sin(t)*Math.sin(t)+1) + 400;
        	double newY = a*Math.sqrt(2)*Math.cos(t)*Math.sin(t)/(Math.sin(t)*Math.sin(t)+1) + 400;
        	
        	/*Simulating inaccuracy of robot:*/
        	int r = (int) Math.sqrt((newX - oldX)*(newX - oldX) + (newY - oldY)*(newY - oldY));
        	int phi = (int) Math.atan2((newY - oldY), (newX - oldX));
        	newX = (int) (r * Math.cos(phi)) + oldX;
        	newY = (int) (r * Math.sin(phi)) + oldY;
        	System.out.println("forward movement: " + r + "cm   turn angle: " + phi + "°");
        	
        	g2d.drawLine((int)oldX, (int)oldY, (int)newX, (int)newY);
        	oldX = newX;
        	oldY = newY;
        }
   } 

    @Override
    public void paintComponent(Graphics g) {
        
        super.paintComponent(g);
        doDrawing(g);
    }    
}

public class LemniscateSimulation extends JFrame {

    public LemniscateSimulation() {

        initUI();
    }
    
    private void initUI() {
        
        setTitle("Lines");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        add(new Surface());
        
        setSize(800, 800);
        setLocationRelativeTo(null);        
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                
                LemniscateSimulation lines = new LemniscateSimulation();
                lines.setVisible(true);
            }
        });
    }
}
