package es.ava.aruco;

public class BoardConfiguration{
	protected int width, height;
	protected int[][] markersId;
	protected int markerSizePix, markerDistancePix;
	
	public BoardConfiguration(int width, int height, int[][] markersId,
			int markerSizePix, int markerDistancePix) {
		super();
		this.width = width;
		this.height = height;
		this.markersId = markersId;
		this.markerSizePix = markerSizePix;
		this.markerDistancePix = markerDistancePix;
	}
}
