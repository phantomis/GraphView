package com.jjoe64.graphview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * a graph series
 */
public class GraphViewSeries {
	static final int DEFAULT_MIN_X = 0;
	static final int DEFAULT_MIN_Y = 0;
	static final int DEFAULT_MAX_X = 100;
	static final int DEFAULT_MAX_Y = 100;		

	/**
	 * one data set for a graph series
	 */
	static public class GraphViewData implements Comparable<GraphViewData> {
		public final double valueX;
		public final double valueY;

		public GraphViewData(double valueX, double valueY) {
			super();
			this.valueX = valueX;
			this.valueY = valueY;
		}
		@Override
		public int compareTo(GraphViewData another) {
			return Double.compare(this.valueX, another.valueX);
		}
	}
		
	final int hashCode;
	final String description;
	final int color;
	private double minX, maxX, minY, maxY;
	private boolean mIsVisible = true;
	final ArrayList<GraphViewData> values = new ArrayList<GraphViewData>();
	private final Comparator<GraphViewData> mYDataComparator = new YDataComparator();
	
	public List<GraphViewData> getValues(){
		return values;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof GraphViewSeries){
			return o.hashCode()==this.hashCode();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return hashCode;
	}
	
	private static class YDataComparator implements Comparator<GraphViewData> {

		@Override
		public int compare(GraphViewData object1, GraphViewData object2) {
			return Double.compare(object1.valueY, object2.valueY);
		}
		
	}

	public GraphViewSeries(List<GraphViewData> values) {
		this(null, 0xff0077cc, values);
	}

	public GraphViewSeries(GraphViewData[] values) {
		this(null, 0xff0077cc, Arrays.asList(values));
	}

	public GraphViewSeries(Integer color, GraphViewData[] values) {
		this(null, (color == null) ? 0xff0077cc : color, Arrays.asList(values));
	}

	public GraphViewSeries(Integer color, List<GraphViewData> values) {
		this(null, (color == null) ? 0xff0077cc : color, values);
	}

	public GraphViewSeries(String description, Integer color, GraphViewData[] values) {
		this(description, (color == null) ? 0xff0077cc : color, Arrays.asList(values));
	}

	public GraphViewSeries(){
		this(null,0xff0077cc,Collections.<GraphViewData> emptyList());
	}
	
	public GraphViewSeries(String description, Integer color, List<GraphViewData> values) {
		this.description = description;
		if (color == null) {
			color = 0xff0077cc; // blue version
		}
		this.color = color;
		this.values.addAll(values);
		Collections.sort(this.values);
		updateAllMinMaxValues();
		hashCode = UUID.randomUUID().toString().hashCode();
	}

	public boolean isVisible(){
		return mIsVisible;
	}
	public void setVisible(boolean value){
		mIsVisible = value;
	}
	
	private void updateAllMinMaxValues(){
		updateMaxX();
		updateMaxY();
		updateMinX();
		updateMinY();			
	}
	
	private void updateMinY(){
		if (values.size()>0){
			minY = Collections.min(values,mYDataComparator).valueY;
		} else {
			minY = DEFAULT_MIN_Y;
		}			
	}
	
	private void updateMaxY(){
		if (values.size()>0){
			maxY = Collections.max(values,mYDataComparator).valueY;
		} else {
			maxY = DEFAULT_MAX_Y;
		}			
	}
	
	private void updateMinX(){
		if (values.size()>0){
			minX = values.get(0).valueX;
		} else {
			minX = DEFAULT_MIN_X;
		}
	}
	private void updateMaxX(){
		if (values.size()>0){
			maxX = values.get(values.size()-1).valueX;
		} else {
			maxX = DEFAULT_MAX_X;
		}
	}
	
	public synchronized void add(GraphViewData data){
		final int lastIndex = values.size()-1;
		if (lastIndex>0) {
			GraphViewData last = this.values.get(lastIndex);
			if (data.valueX <= last.valueX){
				throw new IllegalArgumentException("x value must be larger than the last x values in the series");
			}
		}
		this.values.add(data);
		updateAllMinMaxValues();
	}

	
	public synchronized double getMinX(){
		return minX;
	}
	public synchronized double getMinY(){
		return minY;
	}
	public synchronized double getMaxX(){
		return maxX;
	}		
	public synchronized double getMaxY(){
		return maxY;
	}
}