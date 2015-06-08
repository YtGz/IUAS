/**
0 * This class provides some utils.
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import com.example.iuas.circle.Vector2;

public class Utils {
	public static Mat homography;
	public final static boolean DEBUG = true; // enables debug messages
	public final static int DEBUG_DEVICE = 1; // 1: sysout, 2: textLog.append
	public final static int USE_DEVICE = 2; // 1: USB, 2: Bluetooth
	
	/**
	 * converts the Image Coordinates to Ground Coordinates
	 * @param p
	 * @return
	 */

    public static Point convertImageToGround(Vector2 p){
    	Mat src =  new Mat(1, 1, CvType.CV_32FC2);
        Mat dest = new Mat(1, 1, CvType.CV_32FC2);
        src.put(0, 0, new double[] { p.x, p.y }); // p is a point in image coordinates
        Core.perspectiveTransform(src, dest, homography); //homography is the homography matrix
        Point dest_point = new Point(dest.get(0, 0)[0], dest.get(0, 0)[1]);
       // showLog("convertImageToGround: dest_point: " + dest_point);
        return dest_point;
    }
    
    /**
     * converts the Hsv color values to the Rgba color values
     * @param hsvColor
     * @return
     */
    
    public static Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 3);

        return new Scalar(pointMatRgba.get(0, 0));
    }
    
    /**
     * converts the Rgba color values to the Hsv color values
     * @param rgbColor
     * @return
     */
    public static Scalar converScalarRgb2Hsv(Scalar rgbColor) {
        Mat pointMatHsv = new Mat();
        Mat pointMatRgba = new Mat(1, 1, CvType.CV_8UC3, rgbColor);
        Imgproc.cvtColor(pointMatRgba, pointMatHsv, Imgproc.COLOR_RGB2HSV_FULL, 3);

        return new Scalar(pointMatHsv.get(0, 0));
    }
    
    
    /**
     * checks if two double values are almost equal
     * @param a
     * @param b
     * @param eps
     * @return
     */
    public static boolean almostEqual(double a, double b, double eps){
        return Math.abs(a-b)<eps;
    }
    
    /**
     * calculates the differences in the colorblob boundries
     * @param xMin1
     * @param xMax1
     * @param xMin2
     * @param xMax2
     * @param yMin1
     * @param yMax1
     * @param yMin2
     * @param yMax2
     * @return
     */
    public static Point calculateEps (double xMin1, double xMax1, double xMin2, double xMax2, double yMin1, double yMax1, double yMin2, double yMax2){
    	return new Point((((xMax1 - xMin1) + (xMax2 - xMin2))/2)*2, (((yMax1 - yMin1)+ (yMax2 - yMin2))/2)*.5);
    }
    
	/**
	 * Write debug log on console or mobile phone.
	 */
	public static void showLog(Object text) {
		if (DEBUG) {
			if (DEBUG_DEVICE == 1) {
				System.out.println(text);
			}
			else if (DEBUG_DEVICE == 2) {
				//textLog.append(text + "\n");
			}
		}
	}
}
