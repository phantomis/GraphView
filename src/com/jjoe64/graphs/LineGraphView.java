package com.jjoe64.graphs;

import java.util.List;
import java.util.WeakHashMap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

/**
 * Line Graph View. This draws a line chart.
 * 
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 * 
 *         Copyright (C) 2011 Jonas Gehring Licensed under the GNU Lesser
 *         General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
public class LineGraphView extends GraphView {
	
	/**used to fill the area below the graph line*/
	private Paint mFillPaint;
	
	/**
	 * the graph line is drawn using two lines.
	 * one outer line which is thicker and darker than
	 * the inner lighter line
	 */ 
	private Paint mInnerPaint;
	private Paint mOuterPaint;
	

	/**transforms data points into screen points**/
	private final Matrix mViewPortMatrix = new Matrix();

	/**if true the area below the graph line will be filled*/
	private boolean drawBackground = false;
	
	/** if true graph line will be smoothed by a quadratic fit function*/
	private boolean mSmoothLine = false;

	
	/**helpers to avoit 'new' during draw calls*/
	private final float[] mPoints =new float[2];
	private Path mPath = new Path();
	private Path mClosedPath = new Path();
	
	WeakHashMap<GraphViewSeries, Path> mCachedPath = new WeakHashMap<GraphViewSeries, Path>();
	
	private void init(){
		mFillPaint = new Paint() {
			{
				setStyle(Paint.Style.FILL);
				setARGB(255, 20, 40, 60);
			}
		};

		mInnerPaint = new Paint() {
			{
				setStyle(Paint.Style.STROKE);
				setStrokeCap(Paint.Cap.ROUND);
				setStrokeWidth(2.0f);
				//setMaskFilter(new BlurMaskFilter(3, Blur.SOLID));
				setAntiAlias(true);
			}
		};

		mOuterPaint = new Paint() {
			{
				setStyle(Paint.Style.STROKE);
				setAntiAlias(true);
				setStrokeWidth(5.0f);
				setStrokeCap(Cap.ROUND);
			}
		};
	}
	
	public LineGraphView(Context context){
		super(context);
		init();
	}
	
	public LineGraphView(Context context, AttributeSet set) {
		super(context, set);
		init();
	}
	
	/**
	 * returns the color value which is used to draw the outer path the graph line
	 * @param innerColor
	 * @return color which is half as dark as innerColor
	 */
	private int calculateOuterColor(int innerColor){
		int a = Color.alpha(innerColor);
		int r = Color.red(innerColor);
		int g = Color.green(innerColor);
		int b = Color.blue(innerColor);
		return Color.argb(a, r>>1, g>>1, b>>1);
	}
	/**
	 * returns the color value which is used to fill the area below graph line
	 * @param innerColor
	 * @return transparent version of innerColor
	 */
	private int calculateFillColor(int innerColor){
		int a = Color.alpha(innerColor);
		int r = Color.red(innerColor);
		int g = Color.green(innerColor);
		int b = Color.blue(innerColor);
		return Color.argb(a>>1, r, g, b);
	}
	

	@Override
	public void drawSeries(Canvas canvas, int color, List<GraphViewData> values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart) {
		float startX = 0;
		float lastX = 0;
		float lastY = 0;
		mPath = new Path(); //bug with hardware acceleration forces me to create a new path
		mPath.incReserve(values.size());

		mInnerPaint.setColor(color);
		mOuterPaint.setColor(calculateOuterColor(color));
		mFillPaint.setColor(calculateFillColor(color));

		/*transform data points into screen space*/
		mViewPortMatrix.reset();
		//1. scale
		mViewPortMatrix.postTranslate((float)-minX, (float)-minY);
		mViewPortMatrix.postScale((float)(graphwidth / diffX),(float)(graphheight / diffY));
		//2. flip vertically
		mViewPortMatrix.postScale(1,-1);
		mViewPortMatrix.postTranslate(0,graphheight);
		//3. adjust for borders
		mViewPortMatrix.postTranslate(horstart, border);
		
		for (int i = 0; i < values.size(); i++) {
			
			mPoints[0] = (float)values.get(i).valueX;
			mPoints[1] = (float)values.get(i).valueY;
			mViewPortMatrix.mapPoints(mPoints);

			if (i > 0) {
				if (mSmoothLine){
					mPath.quadTo(lastX, lastY, (mPoints[0]+ lastX) / 2, (mPoints[1]+lastY)/2);
				} else {
					mPath.lineTo(mPoints[0], mPoints[1]);					
				}
			} else {
				startX = mPoints[0];
				mPath.moveTo(mPoints[0], mPoints[1]);
			}
			lastX = mPoints[0];
			lastY = mPoints[1];
		}
		
		if (mSmoothLine){
			mPath.lineTo(mPoints[0], mPoints[1]);			
		}
		
		if (drawBackground) {
			//mClosedPath.reset();
			//mClosedPath.addPath(mPath);
			mClosedPath = new Path(mPath);
			mClosedPath.lineTo(mPoints[0], graphheight + border);
			mClosedPath.lineTo(startX, graphheight + border);
			mClosedPath.close();
			canvas.drawPath(mClosedPath, mFillPaint);
		}
		canvas.drawPath(mPath, mOuterPaint);
		canvas.drawPath(mPath, mInnerPaint);		
	}

	/**
	 * 
	 * @return if true, graph line will be smoothed using a quadratic fit function
	 */
	public boolean getSmoothing(){
		return mSmoothLine;
	}
	
	/**
	 * 
	 * @param value 
	 * 				true to smooth the graph line with a quadratic fit function 
	 */
	public void setSmoothing(boolean value){
		this.mSmoothLine = value;
	}
	
	public boolean getDrawBackground() {
		return drawBackground;
	}

	/**
	 * @param drawBackground
	 *            true to fill the area below the graph line
	 */
	public void setDrawBackground(boolean drawBackground) {
		this.drawBackground = drawBackground;
	}
}
