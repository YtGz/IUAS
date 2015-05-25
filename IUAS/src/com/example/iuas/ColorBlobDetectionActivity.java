/**
 * This class allows the app detect colors by using OpenCV.
 * 
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class ColorBlobDetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
	protected final boolean DEBUG = true; // enables debug messages
	protected final int DEBUG_DEVICE = 1; // 1: sysout, 2: textLog.append --> atm no textLog defined here, so only sysout available
	protected final int USE_DEVICE = 1; // 1: USB, 2: Bluetooth
	
    private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;
    private Scalar				 POINT_COLOR;
    public  static Mat			 homography;
    private boolean				 lockMrgba = false;
    private int					 contoursCountThreshold = 0;//25;
    private enum 			 	 OBJECT_TYPE {BALL, BEACON};
    private enum				 COLOR {YELLOW, RED, GREEN, BLUE}
    private final HashMap<COLOR, Scalar> COLOR_VALUE = new HashMap<COLOR, Scalar>(){{put(COLOR.YELLOW, new Scalar(144, 155, 48)); put(COLOR.RED, new Scalar (155, 44, 50));
    							 put(COLOR.GREEN, new Scalar (33, 153, 109)); put(COLOR.BLUE, new Scalar(0, 182, 255));}}; 
    private enum				 BEACON {YELLOW_RED, RED_YELLOW, BLUE_GREEN, RED_BLUE, BLUE_RED, BLUE_YELLOW, YELLOW_BLUE, RED_GREEN};
    private final HashMap<BEACON, Pair<COLOR, COLOR>> BEACON_COLORS = new HashMap<BEACON, Pair<COLOR, COLOR>>() {{put(BEACON.YELLOW_RED, new Pair(COLOR.YELLOW, COLOR.RED));
    							 put(BEACON.RED_YELLOW, new Pair(COLOR.RED, COLOR.YELLOW)); put(BEACON.BLUE_RED, new Pair(COLOR.BLUE, COLOR.RED));
    							 put(BEACON.RED_BLUE, new Pair(COLOR.RED, COLOR.BLUE)); put(BEACON.BLUE_YELLOW, new Pair(COLOR.BLUE, COLOR.YELLOW));
    							 put(BEACON.YELLOW_BLUE, new Pair(COLOR.YELLOW, COLOR.BLUE)); put(BEACON.BLUE_GREEN, new Pair(COLOR.BLUE, COLOR.GREEN));
    							 put(BEACON.RED_GREEN, new Pair(COLOR.RED, COLOR.GREEN));}};
	private final HashMap<Pair<COLOR, COLOR>, BEACON> COLORS_BEACON = new HashMap<Pair<COLOR, COLOR>, BEACON>() {{
		 for(HashMap.Entry<BEACON, Pair<COLOR, COLOR>> entry : BEACON_COLORS.entrySet()){
		     put(entry.getValue(), entry.getKey());
		 }
	}};
    private final HashMap<BEACON, Point> BEACON_LOC = new HashMap<BEACON, Point>(){{put(BEACON.YELLOW_BLUE, new Point(-125, 125)); put(BEACON.RED_BLUE, new Point(0, 125)); put(BEACON.YELLOW_RED, new Point(125, 125)); put(BEACON.BLUE_GREEN, new Point(-125, 0)); put(BEACON.RED_GREEN, new Point(125, 0)); put(BEACON.BLUE_YELLOW, new Point(-125, -125)); put(BEACON.BLUE_RED, new Point(0, -125)); put(BEACON.RED_YELLOW, new Point(-125, 125));}};
    private int 				 ballCount = 0;
    private int					 beaconCount = 0;
    private int					 contoursCount = 0;
    private HashSet<BEACON>	 	 currentBeacons = new HashSet<BEACON>();


    private CameraBridgeViewBase mOpenCvCameraView;

    /**
     * Internal private class for starting OpenCV.
     */
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(ColorBlobDetectionActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    
    /**
     * Constructor.
     */
    public ColorBlobDetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** 
     * Called when the activity is first created. 
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.color_blob_detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
    }
    
	/**
	 * Write debug log on console or mobile phone.
	 */
	public void showLog(Object text) {
		if (DEBUG) {
			if (DEBUG_DEVICE == 1) {
				System.out.println(text);
			}
			/*else if (DEBUG_DEVICE == 2) {
				textLog.append(text + "\n");
			}*/
		}
	}

    /**
     * On pause camera is disabled.
     */
    @Override
    public void onPause() {   
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    
    /**
     * Resume.
     */
    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }
    
    /**
     * Destroy and disable camera view.
     */
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    
    /**
     * Set camera view components on start.
     */
    public void onCameraViewStarted(int width, int height) {
    	showLog("Cam View: " + mOpenCvCameraView);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(0,0,255,255);
        POINT_COLOR = new Scalar(255,0,0,255);
    }
    
    /**
     * "Release" mRgba variable when camera view stops.
     */
    public void onCameraViewStopped() {
        mRgba.release();
    }
    
    /**
     * On touch try to detect color of object touched and store it in matrix.
     * 
     * @param v
     * @param event
     * @return 
     */
    public boolean onTouch(View v, MotionEvent event) {  	
//        int cols = mRgba.cols();
//        int rows = mRgba.rows();
//
//        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
//        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
//
//        int x = (int)event.getX() - xOffset;
//        int y = (int)event.getY() - yOffset;
//
//        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
//
//        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
//
//        Rect touchedRect = new Rect();
//
//        touchedRect.x = (x>4) ? x-4 : 0;
//        touchedRect.y = (y>4) ? y-4 : 0;
//
//        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
//        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;
//
//        Mat touchedRegionRgba = mRgba.submat(touchedRect);
//
//        Mat touchedRegionHsv = new Mat();
//        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
//
//        // Calculate average color of touched region
//        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
//        int pointCount = touchedRect.width*touchedRect.height;
//        for (int i = 0; i < mBlobColorHsv.val.length; i++)
//            mBlobColorHsv.val[i] /= pointCount;
//
//        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);
//
//        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
//                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
//
//        mDetector.setHsvColor(mBlobColorHsv);
//
//        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
//
//        mIsColorSelected = true;
//
//        touchedRegionRgba.release();
//        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }
    
    /**
     * Determines what happens on getting a camera frame.
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	if(!lockMrgba){
	        mRgba = inputFrame.rgba();
	        
	        if (/*mIsColorSelected*/true) {
	        	ballCount = 0;
	        	contoursCount = 0;
	        	beaconCount = 0;
	        	Point a = new Point();
	        	ArrayList<Pair<MatOfPoint, COLOR>>  contoursAccepted = new ArrayList<Pair<MatOfPoint, COLOR>>();
	        	for(COLOR c : COLOR.values()){
	        		//mDetector.setColorRadius(new Scalar(25,120,120,0));
	        		mDetector.setHsvColor(converScalarRgb2Hsv(COLOR_VALUE.get(c)));
		            mDetector.process(mRgba);
		            List<MatOfPoint> contours = mDetector.getContours();
		            // Count the amount of detected Pixels
		        	int nPixel = 0;
		        	
		        	for(int i = 0; i < contours.size(); i++){
		        		nPixel = contours.get(i).toArray().length;
			        	if(nPixel >= contoursCountThreshold) {
	//			            for (MatOfPoint mp : contours) {
	//			            	double min = Double.MAX_VALUE;
	//			            	for (Point p : mp.toArray()) {
	//			            		if(p.y < min){
	//			            			min = p.y;
	//			            			a = p;
	//			            		}      		
	//			            	}
	//			            	break;
	//			            }
			        		contoursAccepted.add(new Pair<MatOfPoint, COLOR>(contours.get(i), c));
			        	}
		        	}
	        	}
	        	identifyObjects(contoursAccepted);
	            ArrayList<MatOfPoint> l = new ArrayList<MatOfPoint>();
	            l.add(new MatOfPoint(a));
	            ArrayList<MatOfPoint> contoursOnly = new ArrayList<MatOfPoint>();
	            for(Pair<MatOfPoint, COLOR> p : contoursAccepted) {
	            	contoursOnly.add((MatOfPoint) p.first);
	            }
	            Imgproc.drawContours(mRgba, contoursOnly, -1, CONTOUR_COLOR);
	            Imgproc.drawContours(mRgba, l, -1, POINT_COLOR);
	            COLOR c = COLOR.BLUE;
	            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
	            colorLabel.setTo(new Scalar(COLOR_VALUE.get(c).val[0], COLOR_VALUE.get(c).val[1], COLOR_VALUE.get(c).val[2], 1));
	            Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);
	            
	            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
	            mSpectrum.copyTo(spectrumLabel);
        	}
        }
	
        return mRgba;
    }
    
    /**
     * Converts scalar HSV values to RGB values.
     * 
     * @param hsvColor
     * @return
     */
    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 3);

        return new Scalar(pointMatRgba.get(0, 0));
    }
    private Scalar converScalarRgb2Hsv(Scalar rgbColor) {
        Mat pointMatHsv = new Mat();
        Mat pointMatRgba = new Mat(1, 1, CvType.CV_8UC3, rgbColor);
        Imgproc.cvtColor(pointMatRgba, pointMatHsv, Imgproc.COLOR_RGB2HSV_FULL, 3);

        return new Scalar(pointMatHsv.get(0, 0));
    }
    
    /**
     * Creates the homography matrix.
     * 
     * @param mRgba
     * @return
     */
    public Mat getHomographyMatrix(Mat mRgba) {
    	  final Size mPatternSize = new Size(6, 9); // number of inner corners in the used chessboard pattern 
    	  float x = -48.0f; // coordinates of first detected inner corner on chessboard
    	  float y = 309.0f;
    	  float delta = 12.0f; // size of a single square edge in chessboard
    	  LinkedList<Point> PointList = new LinkedList<Point>();
    	 
    	  // Define real-world coordinates for given chessboard pattern:
    	  for (int i = 0; i < mPatternSize.height; i++) {		//swap the loops when switching between landscape and portrait mode
    	    y = 309.0f;
    	    for (int j = 0; j < mPatternSize.width; j++) {
    	      PointList.addLast(new Point(x,y));
    	      y += delta;
    	    }
    	    x += delta;
    	  }
    	  MatOfPoint2f RealWorldC = new MatOfPoint2f();
    	  RealWorldC.fromList(PointList);
    	 
    	  // Detect inner corners of chessboard pattern from image:
    	  Mat gray = new Mat();
    	  Imgproc.cvtColor(mRgba, gray, Imgproc.COLOR_RGBA2GRAY); // convert image to grayscale
    	  MatOfPoint2f mCorners = new MatOfPoint2f();
    	  boolean mPatternWasFound = Calib3d.findChessboardCorners(gray, mPatternSize, mCorners);
    	 
    	  // Calculate homography:
    	  if (mPatternWasFound) {
    		System.out.println("Homography is ready!");
    		lockMrgba = true;
    	    Calib3d.drawChessboardCorners(mRgba, mPatternSize, mCorners, mPatternWasFound); //for visualization
    	    return Calib3d.findHomography(mCorners, RealWorldC);
    	  }
    	  else
    	    return new Mat();
    }
    
    /**
     * Finish on clicking the Back-Button.
     * 
     * @param view
     */
    public void backButtonOnClick(View view) {
    	Intent intent = new Intent();
        intent.putExtra("color", mBlobColorHsv.val);
        setResult(RESULT_OK, intent);
        finish();
    }
    
    /**
     * Try to detect & set homography matrix when "calibrate-h" button is pressed.
     * 
     * @param view
     */
    public void homographyButtonOnClick(View view) {
       /* do {
        	homography = getHomographyMatrix(mRgba);
        } while(homography == null);
    	*/
    	System.out.println("beacon list: " + currentBeacons);
    }
    
    public static boolean almostEqual(double a, double b, double eps){
        return Math.abs(a-b)<eps;
    }
    
    public static Point calculateEps (double xMin1, double xMax1, double xMin2, double xMax2, double yMin1, double yMax1, double yMin2, double yMax2){
    	System.out.println("yMin: " + yMin1 + "yMax: " + yMax1);
    	return new Point((((xMax1 - xMin1) + (xMax2 - xMin2))/2)*2, (((yMax1 - yMin1)+ (yMax2 - yMin2))/2)*.5);
    }
    
    public OBJECT_TYPE[] identifyObjects(ArrayList<Pair<MatOfPoint, COLOR>> it) {
    	double xMin1 = Integer.MAX_VALUE;
    	double xMin2 = Integer.MAX_VALUE;
    	double xMax1 = 0;
    	double xMax2 = 0;
    	double yMin1 = Integer.MAX_VALUE;
    	double yMin2 = Integer.MAX_VALUE;
    	double yMax1 = 0;
    	double yMax2 = 0;
    	boolean[] isBeacon = new boolean[it.size()];
    	
    	currentBeacons.clear();
    	contoursCount = it.size();   	
    	
    	for(int i = 0; i < it.size(); i++ ){
    		xMin1 = Integer.MAX_VALUE;
        	xMax1 = 0;
        	yMin1 = Integer.MAX_VALUE;
        	yMax1 = 0;
    		for(Point p : it.get(i).first.toArray()){
    			xMin1 = p.x < xMin1 ? p.x : xMin1;
    			xMax1 = p.x > xMax1 ? p.x : xMax1;
    			yMin1 = p.y < yMin1 ? p.y : yMin1;
    			yMax1 = p.y > yMax1 ? p.y : yMax1;
    		}
    		for(int j = i+1; j < it.size(); j++){
    			xMin2 = Integer.MAX_VALUE;
            	xMax2 = 0;
            	yMin2 = Integer.MAX_VALUE;
            	yMax2 = 0;
    			for(Point p : it.get(j).first.toArray()){
        			xMin2 = p.x < xMin2 ? p.x : xMin2;
        			xMax2 = p.x > xMax2 ? p.x : xMax2;
        			yMin2 = p.y < yMin2 ? p.y : yMin2;
        			yMax2 = p.y > yMax2 ? p.y : yMax2;
        		}
	    		
    			if(almostEqual(xMin1, xMin2, calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).x)){
    				if(almostEqual(xMax1, xMax2 ,calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).x)){
    					if(almostEqual(yMin1, yMax2, calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).y) && yMax1 > yMax2) {
    						if(COLORS_BEACON.containsKey(new Pair<COLOR, COLOR>(it.get(j).second, it.get(i).second))) {
    							currentBeacons.add(COLORS_BEACON.get(new Pair<COLOR, COLOR>(it.get(j).second, it.get(i).second)));
    							beaconCount = currentBeacons.size();
	    						isBeacon[i] = true;
	    						isBeacon[j] = true;
	    						break;
    						}
    					}
    					else if(almostEqual(yMin2, yMax1 , calculateEps(xMin1, xMax1, xMin2, xMax2, yMin1, yMax1, yMin2, yMax2).y) && yMax1 < yMax2) {
    						if(COLORS_BEACON.containsKey(new Pair<COLOR, COLOR>(it.get(i).second, it.get(j).second))) {
	    						currentBeacons.add(COLORS_BEACON.get(new Pair<COLOR, COLOR>(it.get(i).second, it.get(j).second)));
	    						beaconCount = currentBeacons.size();
	    						isBeacon[i] = true;
	    						isBeacon[j] = true;
	    						break;
    						}
    					}
    				}
    			}
    			
    		}
    	}
    	
    	for(boolean beacon : isBeacon){
    		if(!beacon){
    			ballCount++;
    			//choose active ball
    			//save ball coordinates
    		}
    	}
    	
    
    	System.out.println("Contours count: " + contoursCount);
    	System.out.println("Ball count: " + ballCount);
    	System.out.println("Beacon count: " + beaconCount);
    	
    	
		return null;

    	
   
    	
    }
}
