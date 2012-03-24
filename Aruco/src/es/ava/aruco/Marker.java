package es.ava.aruco;

import java.util.List;
import java.util.Vector;

import min3d.core.Object3dContainer;

import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import es.ava.aruco.exceptions.ExtParamException;

/**
 * Marker detected in an image, it must be a four-squared contour with black border and
 * a valid code inside it. 
 *
 */
public class Marker extends Vector<Point> implements Comparable{

	Object3dContainer object;
	
	private static final long serialVersionUID = 1L;
	protected int id;
	private float ssize;
	private int rotations;
	
	private Code code; // a matrix of integer representing the code (see the class to further explanation)
	
	private Mat mat; // the cvMat of the CANONICAL marker (not the one taken from the capture)
	private Mat Rvec;
	private Mat Tvec;
	
	public Marker(float size){
		id = -1;
		ssize = size;
		// TODO revise how the mats are initialized, better to create them with the proper type
		// code more legible
		code = new Code();
		Rvec = new Mat(3,1,CvType.CV_64FC1);
		Tvec = new Mat(3,1,CvType.CV_64FC1);
		mat = new Mat();
	}
	
	public void draw(Mat in, Scalar color, int lineWidth, boolean writeId){
	    if (size()!=4)
	    	return;

	    // TODO loop¿?
	    for(int i=0;i<4;i++)
	    	Core.line(in, this.get(i), this.get((i+1)%4), color, lineWidth);
	    if(writeId){
	    	String cad = new String();
	    	cad = "id="+id;
	    	// determine the centroid
	    	Point cent = new Point(0,0);
	    	for(int i=0;i<4;i++){
	    		cent.x += this.get(i).x;
	    		cent.y += this.get(i).y;
	    	}
	        cent.x/=4.;
	        cent.y/=4.;
	        Core.putText(in,cad, cent,Core.FONT_HERSHEY_SIMPLEX, 0.5,  color,2);
	    }
	}
	
	/**
	 * returns the perimeter of the marker, the addition of the distances between
	 * consecutive points.
	 * @return the perimeter.
	 */
	public double perimeter(){
		double sum=0;
		for(int i=0;i<size();i++){
			Point current = get(i);
			Point next = get((i+1)%4);
			sum+=Math.sqrt( (current.x-next.x)*(current.x-next.x) +
					(current.y-next.y)*(current.y-next.y));
		}
		return sum;
	}
	
	/**
	 * method to access the id, this only returns the id. Doesn't calculate it.
	 * @return the marker id.
	 */
	public int getMarkerId(){
		return id;
	}
	
	public static Mat createMarkerImage(int id,int size) throws Exception	{
	    if (id>=1024)
	    	throw new Exception("id out of range");
	    Mat marker = new Mat(size,size, CvType.CV_8UC1, new Scalar(0));
	    //for each line, create
	    int swidth=size/7;
	    int ids[]={0x10,0x17,0x09,0x0e};
	    for (int y=0;y<5;y++) {
	        int index=(id>>2*(4-y)) & 0x0003;
	        int val=ids[index];
	        for (int x=0;x<5;x++) {
	            Mat roi=marker.submat((x+1)*swidth, (x+2)*swidth,(y+1)*swidth,(y+2)*swidth);// TODO check
	            if ( (( val>>(4-x) ) & 0x0001) != 0 )
	            	roi.setTo(new Scalar(255));
	            else
	            	roi.setTo(new Scalar(0));
	        }
	    }
	    return marker;
	}
	
	public double[] getGLTransVector(){
		double [] data = new double[Tvec.total()];
		Tvec.get(0, 0, data);
		return data;
	}
	
	public double[] getGLRotVector(){
		double [] data = new double[Rvec.total()];
		Rvec.get(0, 0, data);
		return data;
	}
	
	public void draw3dCube(Mat frame, CameraParameters cp, Scalar color){
		Mat objectPoints = new Mat(8,3, CvType.CV_32FC1);
		double halfSize = ssize/2.0;
		objectPoints.put(0, 0,
				-halfSize, -halfSize, 0,
				-halfSize,  halfSize, 0,
				 halfSize,  halfSize, 0,
				 halfSize, -halfSize, 0,
				
				-halfSize, -halfSize, ssize,
				-halfSize,  halfSize, ssize,
				 halfSize,  halfSize, ssize,
				 halfSize, -halfSize, ssize);
		Mat imagePoints = new Mat();
		Calib3d.projectPoints(objectPoints, Rvec, Tvec, cp.getCameraMatrix(), cp.getDistCoeff(), imagePoints);
		
		List<Point> pts = new Vector<Point>();
		Mat_to_vector_Point(imagePoints, pts);
		// draw
	    for (int i=0;i<4;i++){
	        Core.line(frame ,pts.get(i),pts.get((i+1)%4), color, 2);
	        Core.line(frame,pts.get(i+4),pts.get(4+(i+1)%4), color, 2);
	        Core.line(frame,pts.get(i),pts.get(i+4), color, 2);
	    }	        
	}
	
	public void glGetModelViewMatrix(double[] modelview_matrix)throws ExtParamException{
	    //check if parameters are valid
	    boolean invalid=false;
	    double[] tvec = new double[3];
	    double[] rvec = new double[3];
	    
	    Rvec.get(0, 0, rvec);
	    Tvec.get(0, 0, tvec);

	    for (int i=0;i<3 && !invalid ;i++){
	        if (tvec[i] != -999999) invalid|=false;
	        if (rvec[i] != -999999) invalid|=false;
	    }
	    
	    if (invalid)
	    	throw new ExtParamException("extrinsic parameters are not set Marker.getModelViewMatrix");
	    Mat Rot = new Mat(3,3,CvType.CV_32FC1);
	    Mat Jacob = new Mat();
	    Calib3d.Rodrigues(Rvec, Rot, Jacob);// TODO jacob no se vuelve a usar

	    double[][] para = new double[3][4];
	    double[] rotvec = new double[9];
	    Rot.get(0,0,rotvec);
	    for (int i=0;i<3;i++)
	        for (int j=0;j<3;j++)
	        	para[i][j]=rotvec[3*i+j];
	    //now, add the translation
	    para[0][3]=tvec[0];
	    para[1][3]=tvec[1];
	    para[2][3]=tvec[2];
	    double scale=1;

	    double[] modelview_matrix1 = new double[16];
	    // R1C2
	    modelview_matrix1[0 + 0*4] = para[0][0];
	    modelview_matrix1[0 + 1*4] = para[0][1];
	    modelview_matrix1[0 + 2*4] = para[0][2];
	    modelview_matrix1[0 + 3*4] = para[0][3];
	    // R2
	    modelview_matrix1[1 + 0*4] = para[1][0];
	    modelview_matrix1[1 + 1*4] = para[1][1];
	    modelview_matrix1[1 + 2*4] = para[1][2];
	    modelview_matrix1[1 + 3*4] = para[1][3];
	    // R3
	    modelview_matrix1[2 + 0*4] = -para[2][0];
	    modelview_matrix1[2 + 1*4] = -para[2][1];
	    modelview_matrix1[2 + 2*4] = -para[2][2];
	    modelview_matrix1[2 + 3*4] = -para[2][3];
	    
	    modelview_matrix1[3 + 0*4] = 0.0f;
	    modelview_matrix1[3 + 1*4] = 0.0f;
	    modelview_matrix1[3 + 2*4] = 0.0f;
	    modelview_matrix1[3 + 3*4] = 1.0f;
	    if (scale != 0.0)
	    {
	        modelview_matrix1[12] *= scale;
	        modelview_matrix1[13] *= scale;
	        modelview_matrix1[14] *= scale;
	    }
	    
	    double[] idMat = new double[]{
	    		1, 0, 0, 0,
	    		0, 1, 0, 0,
	    		0, 0, 1, 0,
	    		0, 0, 0, 1
	    };
	    // rotate 90º around the x axis
	    // rotating around x axis in OpenGL is equivalent to
	    // multiply the model matrix by the matrix:
	    // 1, 0, 0, 0, 0, cos(a), sin(a), 0, 0, -sin(a), cos(a), 0, 0, 0, 0, 1
	    double[] auxRotMat = new double[]{
	    	1, 0,  0, 0,
	    	0, 0, 1, 0,
	    	0, -1,  0, 0,
	    	0, 0,  0, 1
	    };
	    // TODO translate to the center of the marker.
	    // 1 0 0 x 0 1 0 y 0 0 1 z 0 0 0 1
	    double[] auxTransMat = new double[]{
	    	1, 0, 0, 0,
	    	0, 1, 0, 0,
	    	0, 0, 1, 0,
	    	-0.034/2.0, 0.034/2.0, -0.034/2.0, 1
	    };
	    double[] modelview_matrix_rotated = new double[16];
	    
	    matrixProduct(modelview_matrix1, auxRotMat, modelview_matrix);
//	    matrixProduct(modelview_matrix_rotated, auxTransMat, modelview_matrix);
	}
	
	protected void setMat(Mat in){
		in.copyTo(mat);
	}
	
	/**
	 * construct the matrix of integers from the mat stored.
	 */
	protected void extractCode(){
		int rows = mat.rows();
		int cols = mat.cols();
		assert(rows == cols);
		Mat grey = new Mat();
		// change the color space if necessary
		if(mat.type() == CvType.CV_8UC1)
			grey = mat;
		else
			Imgproc.cvtColor(mat, grey, Imgproc.COLOR_RGBA2GRAY);
		// apply a threshold
		Imgproc.threshold(grey, grey, 125, 255, Imgproc.THRESH_BINARY|Imgproc.THRESH_OTSU);
		// the swidth is the width of each row
		int swidth = rows/7;
		// we go through all the rows
		for(int y=0;y<7;y++){
			for(int x=0;x<7;x++){
				int Xstart = x*swidth;
				int Ystart = y*swidth;
				Mat square = grey.submat(Xstart, Xstart+swidth, Ystart, Ystart+swidth);
				int nZ = Core.countNonZero(square);
				if(nZ > (swidth*swidth)/2)
					code.set(x, y, 1);
				else
					code.set(x,y,0);
			}
		}
	}
	
	/**
	 * Return the id read in the code inside a marker. Each marker is divided into 7x7 regions
	 * of which the inner 5x5 contain info, the border should always be black. This function
	 * assumes that the code has been extracted previously.
	 * @param in a marker
	 * @return the id of the marker
	 */
	protected int calculateMarkerId(){
		// check all the rotations of code
		Code[] rotations = new Code[4];
		rotations[0] = code;
		int[] dists = new int[4];
		dists[0] = hammDist(rotations[0]);
		int[] minDist = {dists[0],0};
		for(int i=1;i<4;i++){
			// rotate
			rotations[i] = Code.rotate(rotations[i-1]);
			dists[i] = hammDist(rotations[i]);
			if(dists[i] < minDist[0]){
				minDist[0] = dists[i];
				minDist[1] = i;
			}
		}
		this.rotations = minDist[1];
		if(minDist[0] != 0){
			return -1; // matching id not found
		}
		else{
			this.id = mat2id(rotations[minDist[1]]);
		}
		return id;
	}
	
	/**
	 * this functions checks if the whole border of the marker is black
	 * @return true if the border is black, false otherwise
	 */
	protected boolean checkBorder(){
		for(int i=0;i<7;i++){
			// normally we'll only check first and last square
			int inc = 6;
			if(i==0 || i==6)// in first and last row the whole row must be checked
				inc = 1;
			for(int j=0;j<7;j+=inc)
				if(code.get(i, j)==1)
					return false;
		}
		return true;
	}
	
	/**
	 * Calculate 3D position of the marker based on its translation and rotation matrix.
	 * This method fills in these matrix properly.
	 * @param camMatrix
	 * @param distCoeff
	 */
	protected void calculateExtrinsics(Mat camMatrix, Mat distCoeffs, float sizeMeters){// TODO size neccesary?
		// TODO check params
		
		// set the obj 3D points
		double halfSize = sizeMeters/2.0;
		Mat objPoints = new Mat(4,3,CvType.CV_32FC1);// 4 points
		objPoints.put(0, 0,
				 -halfSize, -halfSize, 0,
				 -halfSize,  halfSize, 0,
				  halfSize,  halfSize, 0,
				  halfSize, -halfSize, 0);

		// set the image points (marker points)
		Mat imagePoints = new Mat(4,2,CvType.CV_32FC1);
		double[] buff = new double[8];
        for(int i=0; i<4; i++) {
            buff[i*2]   = this.get(i).x;
            buff[i*2+1] = this.get(i).y;
        }
        imagePoints.put(0, 0, buff);

		Calib3d.solvePnP(objPoints, imagePoints, camMatrix, distCoeffs, Rvec, Tvec);
//		rotateXAxis(Rvec);// TODO necesario??
	}
	
	/**
	 * Performs the product in 2 matrix with column major-order and stores the result
	 * in a third one. Used to carry out the rotation in a modelView matrix in OpenGL.
	 * @param a matrix A
	 * @param b matrix B
	 * @param dst resulting matrix
	 */
	private void matrixProduct(double[] a, double[] b, double dst[]){
        for(int i=0;i<4;i++)
        {
            for(int j=0;j<4;j++)
            {
                dst[i+4*j] = 0;
                for(int k=0;k<4;k++)
                {
                	dst[i+4*j] += a[i+4*k]*b[k+j*4];
                }
            }
        }
	}
	
	private int hammDist(Code code){
		int ids[][] = {
				{1,0,0,0,0},
				{1,0,1,1,1},
				{0,1,0,0,1},
				{0,1,1,1,0}
		};
		int dist = 0;
		for(int y=0;y<5;y++){
			int minSum = Integer.MAX_VALUE;
			// hamming distance to each possible word
			for(int p=0;p<4;p++){
				int sum=0;
				for(int x=0;x<5;x++)
					sum+= code.get(y+1,x+1) == ids[p][x]? 0:1;
				minSum = sum<minSum? sum:minSum;
			}
			dist+=minSum;
		}
		return dist;
	}

	private int mat2id(Code code){
		int val=0;
		for(int y=1;y<6;y++){
			val<<=1;
			if(code.get(y,2) == 1)
				val |= 1;
			val<<=1;
			if(code.get(y,4) == 1)
				val |= 1;
		}
		return val;
	}
	
	/**
	 * Adaption of the org.opencv.utils, in this one the points are set by rows in the mat.
	 * It the receives a CV_32FC2 matrix.
	 * @param m matrix with the points
	 * @param pts output vector with points
	 */
    private static void Mat_to_vector_Point(Mat m, List<Point> pts) {
        if(pts == null)
            throw new java.lang.IllegalArgumentException();
        int rows = m.rows();
        if(!CvType.CV_32FC2.equals(m.type()) ||  m.cols()!=1 )
            throw new java.lang.IllegalArgumentException();

        pts.clear();
        float[] buff = new float[2*rows];
        m.get(0, 0, buff);
        for(int i=0; i<rows; i++) {
            pts.add( new Point(buff[i*2], buff[i*2+1]) );
        }
    }
    
	private void rotateXAxis(Mat rotation){
		Mat R = new Mat(3,3,CvType.CV_32FC1);
		Calib3d.Rodrigues(rotation, R);
		Mat RX = Mat.eye(3, 3, CvType.CV_32FC1);
		double angleRad = Math.PI/2.0;
		RX.put(1,1, Math.cos(angleRad), -Math.sin(angleRad));
		RX.put(2, 1, Math.sin(angleRad), Math.cos(angleRad));
		//TODO multiply, reduce the native calls if posible
		Mat res = new Mat(3,3, CvType.CV_32FC1);
		double[] prod = new double[9];
		double[] a = new double[9];
		float[] b = new float[9];
		R.get(0, 0, a);
		RX.get(0, 0, b);
        for(int i=0;i<3;i++)
            for(int j=0;j<3;j++){
            	prod[3*i+j] = 0;
                for(int k=0;k<3;k++){
                	prod[3*i+j] += a[3*i+k]*b[3*k+j];
                }
                res.put(i, j, prod);
            }
		Calib3d.Rodrigues(R, rotation);
	}
	
	
	public int getRotations(){
		return this.rotations;
	}

	@Override
	public int compareTo(Object another) {
		Marker other = (Marker) another;
		if(id < other.id)
			return -1;
		else if(id > other.id)
			return 1;
		return 0;
	}

	public void set3dObject(Object3dContainer object) throws ExtParamException {
		this.object = object;
		double[] matrix = new double[16];
		this.glGetModelViewMatrix(matrix);
		this.object.setModelViewMatrix(matrix);
	}
}
