package es.ava.aruco;

import java.util.Collections;
import java.util.Vector;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import es.ava.aruco.exceptions.CPException;
import es.ava.aruco.exceptions.ExtParamException;

// TODO eliminate innecessary native calls, for example store the frame info 
// such as type in member fields and call it only once
public class MarkerDetector {
	private enum thresSuppMethod {FIXED_THRES,ADPT_THRES,CANNY};
	
	// TODO are these really fields??
	private double thresParam1, thresParam2;
	private thresSuppMethod thresMethod;
	private Mat grey, thres, thres2, hierarchy2;
	private Vector<Mat> contours2;
		
	private final static double MIN_DISTANCE = 10;
	
	public MarkerDetector(){
		thresParam1 = thresParam2 = 7;
		thresMethod = thresSuppMethod.ADPT_THRES;
		grey = new Mat();
		thres = new Mat();
		thres2 = new Mat();
		hierarchy2 = new Mat();
		contours2 = new Vector<Mat>();
	}

	public static void glMyProjMatrix(CameraParameters cp, Size size,
			double proj_matrix[], double znear, double zfar){
		for(int i=0;i<16;i++)
			proj_matrix[i] = 0.0;
		float[] camMat = new float[9];
		cp.getCameraMatrix().get(0, 0, camMat);
		double iwidth = size.width;
		double iheight = size.height;
//		double x0 = iwidth/2.0;
//		double y0 = iheight/2.0;
		double x0 = 0;
		double y0 = 0;
//		proj_matrix[0] = 2.0*camMat[0]/iwidth;
//		proj_matrix[5] = -2.0*camMat[4]/iheight;
//		proj_matrix[8] = 2.0*(camMat[2]/iwidth)-1.0;
//		proj_matrix[9] = 2.0*(camMat[5]/iheight)-1.0;
//		proj_matrix[10] = -(zfar+znear)/(zfar-znear);
//		proj_matrix[11] = -1.0;
//		proj_matrix[14] = -2.0*zfar*znear / (zfar-znear);
		// OPCION A
//		proj_matrix[0] = 2.0*camMat[0]/iwidth;
//		proj_matrix[5] = -2.0*camMat[4]/iheight;
//		proj_matrix[8] = (iwidth - 2*camMat[2] + 2*x0)/iwidth;
//		proj_matrix[9] = (iheight - 2*camMat[5] + 2*y0)/iheight;
//		proj_matrix[10] = -(zfar+znear)/(zfar-znear);
//		proj_matrix[11] = -1.0;
//		proj_matrix[14] = -2.0*zfar*znear / (zfar-znear);
		// OPCION B
		proj_matrix[0] = 2.0*camMat[0]/iwidth;
		proj_matrix[5] = 2.0*camMat[4]/iheight;
		proj_matrix[8] = 1.0 - (2*x0/iwidth);
		proj_matrix[9] = -1.0 + ( (2*y0+2)/iheight);
		proj_matrix[10] = (zfar+znear)/(znear-zfar);
		proj_matrix[11] = -1.0;
		proj_matrix[14] = 2.0*zfar*znear / (znear-zfar);
		
//		proj_matrix[0] = 2.0*camMat[0]/iwidth;
//		proj_matrix[5] = 2.0*camMat[4]/iheight;
//		proj_matrix[8] = (iwidth - 2*camMat[2] + 2*x0)/iwidth;
//		proj_matrix[9] = (-iheight + 2*camMat[5] + 2*y0)/iheight;
//		proj_matrix[10] = -(zfar+znear)/(zfar-znear);
//		proj_matrix[11] = -1.0;
//		proj_matrix[14] = -2.0*zfar*znear / (zfar-znear);
	}
	
	// default invert = false
    public static void glGetProjectionMatrix(CameraParameters cp, Size orgImgSize, Size size, double proj_matrix[],
    		double gnear, double gfar) throws CPException, ExtParamException{	
    	glGetProjectionMatrix(cp, orgImgSize, size, proj_matrix, gnear, gfar, false);
    }
    
    public static void glGetProjectionMatrix(CameraParameters cp, Size orgImgSize, Size size, double proj_matrix[],
    		double gnear, double gfar, boolean invert) throws CPException, ExtParamException{
        if (cp.isValid()==false)
        	throw new CPException("invalid camera parameters MarkerDetector::glGetProjectionMatrix");
        //Deterime the resized info
        double Ax=(double)(size.width)/(double)(orgImgSize.width);
        double Ay=(double)(size.height)/(double)(orgImgSize.height);
        
        // get the cameraMatrix
        float[] camMat = new float[9];
        cp.getCameraMatrix().get(0,0,camMat);
        double _fx=camMat[0]*Ax;
        double _cx=camMat[2]*Ax;
        double _fy=camMat[4]*Ay;
        double _cy=camMat[5]*Ay;
        double[][] cparam = 
        {
            {_fx,   0,  _cx, 0},
            {  0, _fy,  _cy, 0},
            {  0,   0,    1, 0}
        };

        argConvGLcpara2( cparam, size.width, size.height, gnear, gfar, proj_matrix, invert );
    }
    
	/**
	 * Method to find markers in a Mat given.
	 * @param in input color Mat to find the markers in.
	 * @param detectedMarkers output vector with the markers that have been detected.
	 * @param camMatrix --
	 * @param distCoeff --
	 * @param markerSizeMeters --
	 * @param frameDebug used for debug issues, delete this
	 */
	public void detect(Mat in, Vector<Marker> detectedMarkers, Mat camMatrix, Mat distCoeff,
			float markerSizeMeters, Mat frameDebug){
		Vector<Marker> candidateMarkers = new Vector<Marker>();
		detectedMarkers.clear();
		
		// do the threshold of image and detect contours
		Imgproc.cvtColor(in, grey, Imgproc.COLOR_RGBA2GRAY);
		thresHold(thresMethod, grey, thres);

		// pass a copy because it modifies the src image
		thres.copyTo(thres2);
		Imgproc.findContours(thres2, contours2, hierarchy2, Imgproc.CV_RETR_TREE, Imgproc.CV_CHAIN_APPROX_NONE);
//		Imgproc.drawContours(frameDebug, contours2, -1, new Scalar(255,0,0),2);
		// to each contour analyze if it is a paralelepiped likely to be a marker
		Mat approxCurve = new Mat();
		for(int i=0;i<contours2.size();i++){
			Mat contour = contours2.get(i);
			// first check if it has enough points
			int contourSize = contour.total();
			if(contourSize > in.cols()/5){
				Imgproc.approxPolyDP(contour, approxCurve, contourSize*0.05, true);
				// check the polygon has 4 points
				if(approxCurve.total()== 4){
					// and if it is convex
					if(Imgproc.isContourConvex(approxCurve)){
						// ensure the distance between consecutive points is large enough
						double minDistFound = Double.MAX_VALUE;
						int[] points = new int[8];// [x1 y1 x2 y2 x3 y3 x4 y4]
						approxCurve.get(0,0,points);
						// look for the min distance
						for(int j=0;j<=4;j+=2){
							double d = Math.sqrt( (points[j]-points[(j+2)%4])*(points[j]-points[(j+2)%4]) +
												(points[j+1]-points[(j+3)%4])*(points[j+1]-points[(j+3)%4]));
							if(d<minDistFound)
								minDistFound = d;
						}
						if(minDistFound > MIN_DISTANCE){
							// create a candidate marker
							candidateMarkers.add(new Marker(markerSizeMeters));
							candidateMarkers.lastElement().add(new Point(points[0],points[1]));
							candidateMarkers.lastElement().add(new Point(points[2],points[3]));
							candidateMarkers.lastElement().add(new Point(points[4],points[5]));
							candidateMarkers.lastElement().add(new Point(points[6],points[7]));
						}
					}
				}
			}
		}// all contours processed, now we have the candidateMarkers
		int nCandidates = candidateMarkers.size();
		// sort the points in anti-clockwise order
		for(int i=0;i<nCandidates;i++){
	        // trace a line between the first and second point.
	        // if the third point is at the right side, then the points are anti-clockwise
			Marker marker = candidateMarkers.get(i);
			double dx1 = marker.get(1).x - marker.get(0).x;
			double dy1 = marker.get(1).y - marker.get(0).y;
			double dx2 = marker.get(2).x - marker.get(0).x;
			double dy2 = marker.get(2).y - marker.get(0).y;
			double o = dx1*dy2 - dy1*dx2;
			if(o < 0.0) // the third point is in the left side, we have to swap
				Collections.swap(marker, 1, 3);
		}// points sorted in anti-clockwise order

		// remove the elements whose corners are to close to each other // TODO test without this and see what happens
		Vector<Integer> tooNearCandidates = new Vector<Integer>(); // stores the indexes in the candidateMarkers
										   // i.e [2,3,4,5] the marker 2 is too close to 3 and 4 to 5
		for(int i=0;i<nCandidates;i++){
			Marker toMarker = candidateMarkers.get(i);
			// calculate the average distance of each corner to the nearest corner in the other marker
			for(int j=i+1;j<nCandidates;j++){
				float dist=0;
				Marker fromMarker = candidateMarkers.get(j);
				// unrolling loop
				dist+=Math.sqrt((fromMarker.get(0).x-toMarker.get(0).x)*(fromMarker.get(0).x-toMarker.get(0).x)+
						(fromMarker.get(0).y-toMarker.get(0).y)*(fromMarker.get(0).y-toMarker.get(0).y));

				dist+=Math.sqrt((fromMarker.get(1).x-toMarker.get(1).x)*(fromMarker.get(1).x-toMarker.get(1).x)+
						(fromMarker.get(1).y-toMarker.get(1).y)*(fromMarker.get(1).y-toMarker.get(1).y));
				
				dist+=Math.sqrt((fromMarker.get(2).x-toMarker.get(2).x)*(fromMarker.get(2).x-toMarker.get(2).x)+
						(fromMarker.get(2).y-toMarker.get(2).y)*(fromMarker.get(2).y-toMarker.get(2).y));
				
				dist+=Math.sqrt((fromMarker.get(3).x-toMarker.get(3).x)*(fromMarker.get(3).x-toMarker.get(3).x)+
						(fromMarker.get(3).y-toMarker.get(3).y)*(fromMarker.get(3).y-toMarker.get(3).y));
				dist = dist/4;
				if(dist < MIN_DISTANCE){
					tooNearCandidates.add(i);
					tooNearCandidates.add(j);
				}
			}
		}
		Vector<Integer> toRemove = new Vector<Integer>();// 1 means to remove
		for(int i=0;i<nCandidates;i++)
			toRemove.add(0);
		// set to remove the marker with the smaller perimeter
		for(int i=0;i<tooNearCandidates.size();i+=2){
			Marker first = candidateMarkers.get(tooNearCandidates.get(i));
			Marker second = candidateMarkers.get(tooNearCandidates.get(i+1));
			if(first.perimeter()<second.perimeter())
				toRemove.set(tooNearCandidates.get(i), 1);
			else
				toRemove.set(tooNearCandidates.get(i+1), 1);
		}

		// identify the markers
		for(int i=0;i<nCandidates;i++){
			if(toRemove.get(i) == 0){
				Marker marker = candidateMarkers.get(i);
				Mat canonicalMarker = new Mat();
				warp(in, canonicalMarker, new Size(50,50), marker);
				marker.setMat(canonicalMarker);
				marker.extractCode();
				if(marker.checkBorder()){
					int id = marker.calculateMarkerId();
					if(id != -1){
						detectedMarkers.add(marker);
						// rotate the points of the marker so they are always in the same order no matter the camera orientation
						Collections.rotate(marker, 4-marker.getRotations());
					}
				}
			}
		}
		// TODO refine using pixel accuracy
		
		// now sort by id and check that each marker is only detected once
		Collections.sort(detectedMarkers);
		toRemove.clear();
		for(int i=0;i<detectedMarkers.size();i++)
			toRemove.add(0);
		
		for(int i=0;i<detectedMarkers.size()-1;i++){
			if(detectedMarkers.get(i).id == detectedMarkers.get(i+1).id)
				if(detectedMarkers.get(i).perimeter()<detectedMarkers.get(i+1).perimeter())
					toRemove.set(i, 1);
				else
					toRemove.set(i+1, 1);
		}
		
		for(int i=toRemove.size()-1;i>=0;i--)// done in inverse order in case we need to remove more than one element
			if(toRemove.get(i) == 1)
				detectedMarkers.remove(i);
		
		// detect the position of markers if desired
		for(int i=0;i<detectedMarkers.size();i++){
			detectedMarkers.get(i).calculateExtrinsics(camMatrix, distCoeff, markerSizeMeters);
		}
	}
	
    /**
     * Set the parameters of the threshold method
     * We are currently using the Adptive threshold ee opencv doc of adaptiveThreshold for more info
     * @param param1: blockSize of the pixel neighborhood that is used to calculate a threshold value for the pixel
     * @param param2: The constant subtracted from the mean or weighted mean
     */
	public void setThresholdParams(double p1, double p2){
		thresParam1=p1;
		thresParam2=p2;
	}
	
    /**
     * Get the parameters of the threshold method
     * they will be returned as a 2 items double array.
     */
	public double[] getThresholdParams(){
		double[] ret = {thresParam1,thresParam2};
		return ret;
	}
	
	/**
	 * sets the method to be used in the threshold necessary to the marker detection.
	 * @param method must be a supported method.
	 */
	public void setThresholdMethod(thresSuppMethod method){
		thresMethod = method;
	}
	
	/**
	 * returns the method being used to threshold the image.
	 * @return the method used.
	 */
	public thresSuppMethod getThresholdMethod(){
		return thresMethod;
	}
	
	// TODO revise this code
	private void thresHold(thresSuppMethod method, Mat src, Mat dst){
		switch(method){
		case FIXED_THRES:
			Imgproc.threshold(src, dst, thresParam1,255, Imgproc.THRESH_BINARY_INV);
			break;
		case ADPT_THRES:
			Imgproc.adaptiveThreshold(src,dst,255.0,Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
					Imgproc.THRESH_BINARY_INV,(int)thresParam1,thresParam2);
			break;
		case CANNY:
			Imgproc.Canny(src, dst, 10, 220);// TODO this parameters??
			break;
		}
	}
	
	/**
	 * This fits a mat containing 4 vertices captured through the camera
	 * into a canonical mat.
	 * @param in the frame captured
	 * @param out the canonical mat
	 * @param size the size of the canonical mat we want to create
	 * @param points the coordinates of the points in the "in" mat 
	 */
	private void warp(Mat in, Mat out, Size size, Vector<Point> points){
		Mat pointsIn = new Mat(4,1,CvType.CV_32FC2);
		Mat pointsRes = new Mat(4,1,CvType.CV_32FC2);
		pointsIn.put(0,0, points.get(0).x,points.get(0).y,
						  points.get(1).x,points.get(1).y,
						  points.get(2).x,points.get(2).y,
						  points.get(3).x,points.get(3).y);
		pointsRes.put(0,0, 0,0,
						   size.width-1,0,
						   size.width-1,size.height-1,
						   0,size.height-1);
		Mat m = new Mat();
		m = Imgproc.getPerspectiveTransform(pointsIn, pointsRes);
		Imgproc.warpPerspective(in, out, m, size);
	}
	
	private static void argConvGLcpara2(double[][] cparam, double width, double height, double gnear,
			double gfar, double[] m, boolean invert) throws ExtParamException{
		double[][] icpara = new double[3][4];
		double[][] trans  = new double[3][4];
		double[][] p      = new double[4][4];
		double[][] q      = new double[4][4];
		
		cparam[0][2] *= -1f;
		cparam[1][2] *= -1f;
		cparam[2][2] *= -1f;
		
		if(arParamDecompMat(cparam, icpara, trans)<0)
			throw new ExtParamException("parameter error, argConvGLcpara2");
		for(int i=0;i<3;i++)
			for(int j=0;j<3;j++)
				p[i][j] = icpara[i][j]/icpara[2][2];
	    q[0][0] = (2.0 * p[0][0] / width);
	    q[0][1] = (2.0 * p[0][1] / width);
	    q[0][2] = ((2.0 * p[0][2] / width)  - 1.0);
	    q[0][3] = 0.0;

	    q[1][0] = 0.0;
	    q[1][1] = (2.0 * p[1][1] / height);
	    q[1][2] = ((2.0 * p[1][2] / height) - 1.0);
	    q[1][3] = 0.0;

	    q[2][0] = 0.0;
	    q[2][1] = 0.0;
	    q[2][2] = (gfar + gnear)/(gfar - gnear);
	    q[2][3] = -2.0 * gfar * gnear / (gfar - gnear);

	    q[3][0] = 0.0;
	    q[3][1] = 0.0;
	    q[3][2] = 1.0;
	    q[3][3] = 0.0;
	    
	    for (int i = 0; i < 4; i++ )
	    {
	        for (int j = 0; j < 3; j++ )
	        {
	            m[i+j*4] = q[i][0] * trans[0][j]
	                       + q[i][1] * trans[1][j]
	                       + q[i][2] * trans[2][j];
	        }
	        m[i+3*4] = q[i][0] * trans[0][3]
	                   + q[i][1] * trans[1][3]
	                   + q[i][2] * trans[2][3]
	                   + q[i][3];
	    }

	    if (!invert)
	    {
	        m[13]=-m[13] ;
	        m[1]=-m[1];
	        m[5]=-m[5];
	        m[9]=-m[9];
	    }
	}
	
	private static int arParamDecompMat(double[][] source, double[][] cpara, double[][] trans){
	    int        r, c;
	    double[][] Cpara = new double[3][4];
	    double     rem1, rem2, rem3;

	    if ( source[2][3] >= 0 )
	    {
	        for ( r = 0; r < 3; r++ )
	        {
	            for ( c = 0; c < 4; c++ )
	            {
	                Cpara[r][c] = source[r][c];
	            }
	        }
	    }
	    else
	    {
	        for ( r = 0; r < 3; r++ )
	        {
	            for ( c = 0; c < 4; c++ )
	            {
	                Cpara[r][c] = -(source[r][c]);
	            }
	        }
	    }

	    for ( r = 0; r < 3; r++ )
	    {
	        for ( c = 0; c < 4; c++ )
	        {
	            cpara[r][c] = 0.0;
	        }
	    }
	    cpara[2][2] = norm( Cpara[2][0], Cpara[2][1], Cpara[2][2] );
	    trans[2][0] = Cpara[2][0] / cpara[2][2];
	    trans[2][1] = Cpara[2][1] / cpara[2][2];
	    trans[2][2] = Cpara[2][2] / cpara[2][2];
	    trans[2][3] = Cpara[2][3] / cpara[2][2];

	    cpara[1][2] = dot( trans[2][0], trans[2][1], trans[2][2],
	                       Cpara[1][0], Cpara[1][1], Cpara[1][2] );
	    rem1 = Cpara[1][0] - cpara[1][2] * trans[2][0];
	    rem2 = Cpara[1][1] - cpara[1][2] * trans[2][1];
	    rem3 = Cpara[1][2] - cpara[1][2] * trans[2][2];
	    cpara[1][1] = norm( rem1, rem2, rem3 );
	    trans[1][0] = rem1 / cpara[1][1];
	    trans[1][1] = rem2 / cpara[1][1];
	    trans[1][2] = rem3 / cpara[1][1];

	    cpara[0][2] = dot( trans[2][0], trans[2][1], trans[2][2],
	                       Cpara[0][0], Cpara[0][1], Cpara[0][2] );
	    cpara[0][1] = dot( trans[1][0], trans[1][1], trans[1][2],
	                       Cpara[0][0], Cpara[0][1], Cpara[0][2] );
	    rem1 = Cpara[0][0] - cpara[0][1]*trans[1][0] - cpara[0][2]*trans[2][0];
	    rem2 = Cpara[0][1] - cpara[0][1]*trans[1][1] - cpara[0][2]*trans[2][1];
	    rem3 = Cpara[0][2] - cpara[0][1]*trans[1][2] - cpara[0][2]*trans[2][2];
	    cpara[0][0] = norm( rem1, rem2, rem3 );
	    trans[0][0] = rem1 / cpara[0][0];
	    trans[0][1] = rem2 / cpara[0][0];
	    trans[0][2] = rem3 / cpara[0][0];

	    trans[1][3] = (Cpara[1][3] - cpara[1][2]*trans[2][3]) / cpara[1][1];
	    trans[0][3] = (Cpara[0][3] - cpara[0][1]*trans[1][3]
	                   - cpara[0][2]*trans[2][3]) / cpara[0][0];

	    for ( r = 0; r < 3; r++ )
	    {
	        for ( c = 0; c < 3; c++ )
	        {
	            cpara[r][c] /= cpara[2][2];
	        }
	    }

	    return 0;
	}
	
	private static double norm( double a, double b, double c )
	{
	    return( Math.sqrt( a*a + b*b + c*c ) );
	}
	
	private static double dot( double a1, double a2, double a3,
            double b1, double b2, double b3 )
	{
		return( a1 * b1 + a2 * b2 + a3 * b3 );
	}
}
