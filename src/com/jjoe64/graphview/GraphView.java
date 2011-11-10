package com.jjoe64.graphview;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.jjoe64.graphs.LineGraphView;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;
import com.jjoe64.graphview.compatible.ScaleGestureDetector;

/**
 * GraphView is a Android View for creating zoomable and scrollable graphs. This
 * is the abstract base class for all graphs. Extend this class and implement
 * {@link #drawSeries(Canvas, float, float, float, double, double, double, double, float)}
 * to display a custom graph. Use {@link LineGraphView} for creating a line
 * chart.
 * 
 * @author jjoe64 - jonas gehring - http://www.jjoe64.com
 * 
 *         Copyright (C) 2011 Jonas Gehring Licensed under the GNU Lesser
 *         General Public License (LGPL) http://www.gnu.org/licenses/lgpl.html
 */
abstract public class GraphView extends LinearLayout {
	static final private class GraphViewConfig {
		static final float BORDER = 20;
		static final float VERTICAL_LABEL_WIDTH = 100;
		static final float HORIZONTAL_LABEL_HEIGHT = 80;
	}
	
	public static interface ViewportChangeListener {
		void onViewportChanged(double start, double size);
	}
	
	private boolean mIsBeingDragged = false;
	/**
	 * Position of the last motion event.
	 */
	private float mLastMotionX;
	private Scroller mScroller;
	private int mTouchSlop;
	private int mMinimumVelocity;
	private int mMaximumVelocity;
	private ViewportChangeListener mViewPortListener;

	/**
	 * Determines speed during touch scrolling
	 */
	private VelocityTracker mVelocityTracker;
	
	
	public void setViewportListener(ViewportChangeListener listener){
		mViewPortListener = listener;
	}

	private boolean canScroll() {
		final double diffX = getMaxX(true) -getMinX(true);
		if (scrollable && viewportSize < diffX) {
			return true;
		}
		return false;
	}

	private class GraphViewContentView extends View {
		private float graphwidth;
		double mScale = 1;

		
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			super.onLayout(changed, left, top, right, bottom);
			if (changed){
				mScale = (right-left) / viewportSize;
			}
		}

		public void onViewportChanged(){
			synchronized (GraphView.this) {
				if (viewportSize>0 && getWidth() > 0){
					mScale = getWidth() / viewportSize;
					horlabels = null;
					this.invalidate();
				}
			}
		}
		
		
		/**
		 * @param context
		 */
		public GraphViewContentView(Context context) {
			super(context);
			setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		}

		private int mLastScroll;
		
		public void computeScroll() {

			if (mScroller.computeScrollOffset()) {
				int x = mScroller.getCurrX();
				onMoveGesture((float) (mLastScroll - x));
				mLastScroll = x;
				// Keep on drawing until the animation has finished.
				postInvalidate();
			} 
			
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas) {
			synchronized (GraphView.this) {
				computeScroll();

				// normal
				paint.setStrokeWidth(0);

				float border = GraphViewConfig.BORDER;
				float horstart = 0;
				float height = getHeight();
				float width = getWidth() - 1;
				float graphheight = height - (2 * border);
				graphwidth = width;

				if (horlabels == null) {
					horlabels = generateHorlabels(graphwidth);
				}
				if (verlabels == null) {
					verlabels = generateVerlabels(graphheight);
				}

				// vertical lines
				paint.setTextAlign(Align.LEFT);
				int vers = verlabels.length - 1;
				for (int i = 0; i < verlabels.length; i++) {
					paint.setColor(Color.DKGRAY);
					float y = ((graphheight / vers) * i) + border;
					canvas.drawLine(horstart, y, width, y, paint);
				}

				// horizontal labels + lines
				int hors = horlabels.length - 1;
				for (int i = 0; i < horlabels.length; i++) {
					paint.setColor(Color.DKGRAY);
					float x = ((graphwidth / hors) * i) + horstart;
					canvas.drawLine(x, height - border, x, border, paint);
					paint.setTextAlign(Align.CENTER);
					if (i == horlabels.length - 1)
						paint.setTextAlign(Align.RIGHT);
					if (i == 0)
						paint.setTextAlign(Align.LEFT);
					paint.setColor(Color.WHITE);
					canvas.drawText(horlabels[i], x, height - 4, paint);
				}

				paint.setTextAlign(Align.CENTER);
				canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

				if (graphSeries.size() > 0) {
					double maxY = getMaxY();
					double minY = getMinY();
					double diffY = maxY - minY;
					double maxX = getMaxX(false);
					double minX = getMinX(false);
					double diffX = maxX - minX;

					if (maxY != minY) {
						paint.setStrokeCap(Paint.Cap.ROUND);
						paint.setStrokeWidth(3);

						for (int i = 0; i < graphSeries.size(); i++) {
							if (graphSeries.get(i).isVisible()) {
								paint.setColor(graphSeries.get(i).color);
								List<GraphViewData> valuesToDraw = _values(i);
								if (valuesToDraw != null) {
									drawSeries(canvas, graphSeries.get(i).color, valuesToDraw, graphwidth, graphheight, border, minX, minY, diffX, diffY, horstart);
								}
							}
						}

						if (showLegend)
							drawLegend(canvas, height, width);
					}

				}
			}
		}
		public void fling(int velocityX) {
			double mTotalGraphWidth;
			mTotalGraphWidth = (getMaxX(true)-getMinX(true)) * mScale;

			int width = (int) mTotalGraphWidth;
			int right = (int) graphwidth;
			//convert viewportstart to screen coords
			final double screenViewPortStart = ((viewportStart - getMinX(true))*mScale);
			mLastScroll = (int) screenViewPortStart;
		
			mScroller.fling((int) (screenViewPortStart), 0, velocityX, 0, 0,  width-right, 0, 0);

			invalidate();
		}

		
		private void onMoveGesture(float f) {
			// view port update
			if (viewportSize != 0 && graphSeries.size() != 0) {
				double maxX = getMaxX(true);
				double minX = getMinX(true);
				if (viewportStart + viewportSize >= maxX && f < 0) {
					return;
				}
				viewportStart -= f / mScale;
				// minimal and maximal view limit
				if (viewportStart < minX) {
					viewportStart = minX;
				} else if (viewportStart + viewportSize > maxX) {
					viewportStart = maxX - viewportSize;
				}
				// labels have to be regenerated
				horlabels = null;
				verlabels = null;
				GraphView.this.onViewportChanged(false);
				viewVerLabels.invalidate();
			}
		}

	}


	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {		
		/*
		 * This method JUST determines whether we want to intercept the motion.
		 * If we return true, onMotionEvent will be called and we do the actual
		 * scrolling there.
		 */

		/*
		 * Shortcut the most recurring case: the user is in the dragging state
		 * and he is moving his finger. We want to intercept this motion.
		 */
		final int action = ev.getAction();
		if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged)) {
			return true;
		}

		if (!canScroll()) {
			mIsBeingDragged = false;
			return false;
		}
		


		final float x = ev.getX();

		switch (action) {
		case MotionEvent.ACTION_MOVE:
			/*
			 * mIsBeingDragged == false, otherwise the shortcut would have
			 * caught it. Check whether the user has moved far enough from his
			 * original down touch.
			 */

			/*
			 * Locally do absolute value. mLastMotionX is set to the x value of
			 * the down event.
			 */
			final int xDiff = (int) Math.abs(x - mLastMotionX);
			if (xDiff > mTouchSlop) {
				mIsBeingDragged = true;
			}
			break;

		case MotionEvent.ACTION_DOWN:
			/* Remember location of down touch */
			mLastMotionX = x;

			/*
			 * If being flinged and user touches the screen, initiate drag;
			 * otherwise don't. mScroller.isFinished should be false when being
			 * flinged.
			 */
			mIsBeingDragged = !mScroller.isFinished();
			break;

		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			/* Release the drag */
			mIsBeingDragged = false;
			break;
		}

		/*
		 * The only time we want to intercept motion events is if we are in the
		 * drag or zoom mode.
		 */
		return mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		// first scale
		if (scalable && scaleDetector != null) {
			scaleDetector.onTouchEvent(ev);
			scaleDetector.isInProgress();
		}
		
		if (!canScroll()) {
			return true;
		}

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		final int action = ev.getAction();
		final float x = ev.getX();

		switch (action) {
		case MotionEvent.ACTION_DOWN:
			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */
			if (!mScroller.isFinished()) {
				mScroller.abortAnimation();
			}

			// Remember where the motion event started
			mLastMotionX = x;
			break;
		case MotionEvent.ACTION_MOVE:
			// Scroll to follow the motion event
			final int deltaX = (int) (x - mLastMotionX);
			mLastMotionX = x;
			mContentView.onMoveGesture(deltaX);
			mContentView.invalidate();
			break;
		case MotionEvent.ACTION_UP:
			final VelocityTracker velocityTracker = mVelocityTracker;
			velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
			int initialVelocity = (int) velocityTracker.getXVelocity();

			if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
				mContentView.fling(-initialVelocity);
			}

			if (mVelocityTracker != null) {
				mVelocityTracker.recycle();
				mVelocityTracker = null;
			}
		}

		return true;
	}

	public enum LegendAlign {
		TOP, MIDDLE, BOTTOM
	}
	
	

	private class VerLabelsView extends View {
		/**
		 * @param context
		 */
		public VerLabelsView(Context context) {
			super(context);
			setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 10));
		}

		/**
		 * @param canvas
		 */
		@Override
		protected void onDraw(Canvas canvas) {
			synchronized (GraphView.this) {

				// normal
				paint.setStrokeWidth(0);

				float border = GraphViewConfig.BORDER;
				float height = getHeight();
				float graphheight = height - (2 * border);

				if (verlabels == null) {
					verlabels = generateVerlabels(graphheight);
				}

				// vertical labels
				paint.setTextAlign(Align.LEFT);
				int vers = verlabels.length - 1;
				for (int i = 0; i < verlabels.length; i++) {
					float y = ((graphheight / vers) * i) + border;
					paint.setColor(Color.WHITE);
					canvas.drawText(verlabels[i], 0, y, paint);
				}
			}
		}

	}

	protected Paint paint;
	private String[] horlabels;
	private String[] verlabels;
	private String title;
	private boolean scrollable;
	private double viewportStart;
	private double viewportSize;
	private View viewVerLabels;
	private ScaleGestureDetector scaleDetector;
	private boolean scalable;
	private NumberFormat numberformatter;
	private List<GraphViewSeries> graphSeries;
	private boolean showLegend = false;
	private float legendWidth = 120;
	private LegendAlign legendAlign = LegendAlign.MIDDLE;
	private boolean manualYAxis;
	private double manualMaxYValue;
	private double manualMinYValue;
	private GraphViewContentView mContentView;

	/**
	 * 
	 * @param context
	 * @param title
	 *            [optional]
	 */
	public GraphView(Context context) {
		super(context);
		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		init(context);
	}

	public GraphView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	private void init(Context context) {
		title = "";

		paint = new Paint();
		graphSeries = new ArrayList<GraphViewSeries>();

		viewVerLabels = new VerLabelsView(context);
		addView(viewVerLabels);

		mContentView = new GraphViewContentView(context);
		addView(mContentView, new LayoutParams(LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT, 1));
		
		mScroller = new Scroller(getContext());
		setFocusable(true);
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		final ViewConfiguration configuration = ViewConfiguration.get(context);
		mTouchSlop = configuration.getScaledTouchSlop();
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

	}

	private List<GraphViewData> _values(int idxSeries) {
		if (viewportStart == 0 && viewportSize == 0) {
			// all data
			return graphSeries.get(idxSeries).values;
		} else {
			int size = graphSeries.get(idxSeries).values.size();

			GraphViewData dummyData = new GraphViewData(viewportStart, 0);
			int start = Math.abs(Collections.binarySearch(graphSeries.get(idxSeries).values, dummyData));
			// series has no values inside current viewport
			if (start > size) {
				return null;
			}

			start = Math.max(start-2, 0);

			dummyData = new GraphViewData(viewportStart + viewportSize, 0);
			int end = Math.abs(Collections.binarySearch(graphSeries.get(idxSeries).values, dummyData));

			end = Math.min(end+1, size);

			return graphSeries.get(idxSeries).values.subList(start, end);

		}
	}

	protected void onAddSeries(GraphViewSeries series) {

	}

	protected void onRemoveSeries(GraphViewSeries series) {

	}

	protected void onAddToSeries(GraphViewSeries series, GraphViewData data) {

	}

	public void addSeries(GraphViewSeries series) {
		graphSeries.add(series);
		onAddSeries(series);
		mContentView.invalidate();
	}

	public void removeSeries(GraphViewSeries series) {
		graphSeries.remove(series);
		onRemoveSeries(series);
		mContentView.invalidate();
	}

	public void toggleSeries(GraphViewSeries series) {
		// verlabels = null;
		if (graphSeries.contains(series)) {
			boolean visible = series.isVisible();
			series.setVisible(!visible);
			horlabels = null;
			this.mContentView.invalidate();
		}
	}

	public synchronized void addToSeries(int index, GraphViewData data) {
		if (graphSeries.size()==0){
			GraphViewSeries series = new GraphViewSeries(new GraphViewData[]{data});
			this.addSeries(series);
		} else {
			GraphViewSeries series = graphSeries.get(index);
			series.add(data);
			onAddToSeries(series, data);
			horlabels = null;
			verlabels = null;
			this.mContentView.invalidate();
		}		
	}

	public GraphViewData getData(int seriesIndex, int dataIndex) {
		return graphSeries.get(seriesIndex).values.get(dataIndex);
	}

	public GraphViewData getLastData(int seriesIndex) {
		return graphSeries.get(seriesIndex).values.get(getSeriesSize(seriesIndex) - 1);
	}

	public int getSeriesSize(int seriesIndex) {
		return graphSeries.get(seriesIndex).values.size();
	}

	protected void drawLegend(Canvas canvas, float height, float width) {
		int shapeSize = 15;

		// rect
		paint.setARGB(180, 100, 100, 100);
		float legendHeight = (shapeSize + 5) * graphSeries.size() + 5;
		float lLeft = width - legendWidth - 10;
		float lTop;
		switch (legendAlign) {
		case TOP:
			lTop = 10;
			break;
		case MIDDLE:
			lTop = height / 2 - legendHeight / 2;
			break;
		default:
			lTop = height - GraphViewConfig.BORDER - legendHeight - 10;
		}
		float lRight = lLeft + legendWidth;
		float lBottom = lTop + legendHeight;
		canvas.drawRoundRect(new RectF(lLeft, lTop, lRight, lBottom), 8, 8, paint);

		for (int i = 0; i < graphSeries.size(); i++) {
			paint.setColor(graphSeries.get(i).color);
			canvas.drawRect(new RectF(lLeft + 5, lTop + 5 + (i * (shapeSize + 5)), lLeft + 5 + shapeSize, lTop + ((i + 1) * (shapeSize + 5))), paint);
			if (graphSeries.get(i).description != null) {
				paint.setColor(Color.WHITE);
				paint.setTextAlign(Align.LEFT);
				canvas.drawText(graphSeries.get(i).description, lLeft + 5 + shapeSize + 5, lTop + shapeSize + (i * (shapeSize + 5)), paint);
			}
		}
	}

	abstract public void drawSeries(Canvas canvas, int color, List<GraphViewData> values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY,
			float horstart);

	/**
	 * formats the label can be overwritten
	 * 
	 * @param value
	 *            x and y values
	 * @param isValueX
	 *            if false, value y wants to be formatted
	 * @return value to display
	 */
	protected String formatLabel(double value, boolean isValueX) {
		if (numberformatter == null) {
			numberformatter = NumberFormat.getNumberInstance();
			double highestvalue = getMaxY();
			double lowestvalue = getMinY();
			if (highestvalue - lowestvalue < 0.1) {
				numberformatter.setMaximumFractionDigits(6);
			} else if (highestvalue - lowestvalue < 1) {
				numberformatter.setMaximumFractionDigits(4);
			} else if (highestvalue - lowestvalue < 20) {
				numberformatter.setMaximumFractionDigits(3);
			} else if (highestvalue - lowestvalue < 100) {
				numberformatter.setMaximumFractionDigits(1);
			} else {
				numberformatter.setMaximumFractionDigits(0);
			}
		}
		return numberformatter.format(value);
	}

	private String[] generateHorlabels(float graphwidth) {
		int numLabels = (int) (graphwidth / GraphViewConfig.VERTICAL_LABEL_WIDTH);
		String[] labels = new String[numLabels + 1];
		double min = getMinX(false);
		double max = getMaxX(false);
		for (int i = 0; i <= numLabels; i++) {
			labels[i] = formatLabel(min + ((max - min) * i / numLabels), true);
		}
		return labels;
	}

	synchronized private String[] generateVerlabels(float graphheight) {
		int numLabels = (int) (graphheight / GraphViewConfig.HORIZONTAL_LABEL_HEIGHT);
		String[] labels = new String[numLabels + 1];
		double min = getMinY();
		double max = getMaxY();
		for (int i = 0; i <= numLabels; i++) {
			labels[numLabels - i] = formatLabel(min + ((max - min) * i / numLabels), false);
		}
		return labels;
	}

	public LegendAlign getLegendAlign() {
		return legendAlign;
	}

	public float getLegendWidth() {
		return legendWidth;
	}

	private synchronized double getMaxX(boolean ignoreViewport) {
		// if viewport is set, use this
		if (!ignoreViewport && viewportSize != 0) {
			return viewportStart + viewportSize;
		} else {
			double maxX = Double.MIN_VALUE;
			for (GraphViewSeries series : graphSeries) {
				if (series.getMaxX() > maxX) {
					maxX = series.getMaxX();
				}
			}
			if (graphSeries.size() == 0) {
				maxX = GraphViewSeries.DEFAULT_MAX_X;
			}

			return maxX;
		}
	}

	private synchronized double getMaxY() {
		double largest;
		if (manualYAxis) {
			largest = manualMaxYValue;
		} else {
			largest = Double.MIN_VALUE;
			for (GraphViewSeries series : graphSeries) {
				if (series.getMaxY() > largest) {
					largest = series.getMaxY();
				}
			}
			if (graphSeries.size() == 0) {
				largest = GraphViewSeries.DEFAULT_MAX_Y;
			}
		}
		return largest;
	}

	private synchronized double getMinX(boolean ignoreViewport) {
		// if viewport is set, use this
		if (!ignoreViewport && viewportSize != 0) {
			return viewportStart;
		} else {
			double minX = Double.MAX_VALUE;
			for (GraphViewSeries series : graphSeries) {
				if (series.getMinX() < minX) {
					minX = series.getMinX();
				}
			}
			if (graphSeries.size() == 0) {
				minX = GraphViewSeries.DEFAULT_MIN_X;
			}

			return minX;
		}
	}

	private synchronized double getMinY() {
		double smallest;
		if (manualYAxis) {
			smallest = manualMinYValue;
		} else {
			smallest = Double.MAX_VALUE;
			for (GraphViewSeries series : graphSeries) {
				if (series.getMinY() < smallest) {
					smallest = series.getMinY();
				}
			}
			if (graphSeries.size() == 0) {
				smallest = GraphViewSeries.DEFAULT_MIN_Y;
			}
		}
		return smallest;
	}

	public boolean isScrollable() {
		return scrollable;
	}

	public boolean isShowLegend() {
		return showLegend;
	}

	/**
	 * set's static horizontal labels (from left to right)
	 * 
	 * @param horlabels
	 *            if null, labels were generated automatically
	 */
	public void setHorizontalLabels(String[] horlabels) {
		this.horlabels = horlabels;
	}

	public void setLegendAlign(LegendAlign legendAlign) {
		this.legendAlign = legendAlign;
	}

	public void setLegendWidth(float legendWidth) {
		this.legendWidth = legendWidth;
	}

	/**
	 * you have to set the bounds {@link #setManualYAxisBounds(double, double)}.
	 * That automatically enables manualYAxis-flag. if you want to disable the
	 * menual y axis, call this method with false.
	 * 
	 * @param manualYAxis
	 */
	public void setManualYAxis(boolean manualYAxis) {
		this.manualYAxis = manualYAxis;
	}

	/**
	 * set manual Y axis limit
	 * 
	 * @param max
	 * @param min
	 */
	public void setManualYAxisBounds(double max, double min) {
		manualMaxYValue = max;
		manualMinYValue = min;
		manualYAxis = true;
	}

	public boolean isManualYAxisBounds(){
		return manualYAxis;
	}
	public double getManualMinYValue(){
		return manualMinYValue;
	}
	public double getManualMaxYValue(){
		return manualMaxYValue;
	}
	
	
	/**
	 * this forces scrollable = true
	 * 
	 * @param scalable
	 */
	synchronized public void setScalable(boolean scalable) {
		this.scalable = scalable;
		if (scalable == true && scaleDetector == null) {
			scrollable = true; // automatically forces this
			scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
				public boolean onScale(ScaleGestureDetector detector) {
					final double newSize = viewportSize * detector.getScaleFactor();
					final double diff = newSize - viewportSize;
					final double minX = getMinX(true);
					final double maxX = getMaxX(true);
					viewportStart += diff / 2;
					viewportSize -= diff;
					if (diff < 0) {
						// viewportStart must not be < minX
						if (viewportStart < minX) {
							viewportStart = minX;
						}

						// viewportStart + viewportSize must not be > maxX
						double overlap = viewportStart + viewportSize - maxX;
						if (overlap > 0) {
							// scroll left
							if (viewportStart - overlap > minX) {
								viewportStart -= overlap;
							} else {
								// maximal scale
								viewportStart = minX;
								viewportSize = maxX - viewportStart;
							}
						}
					}
					verlabels = null;
					horlabels = null;
					numberformatter = null;
					mContentView.onViewportChanged();
					onViewportChanged();
					invalidate();
					viewVerLabels.invalidate();
					return true;
				}
			});
		}
	}

	/**
	 * the user can scroll (horizontal) the graph. This is only useful if you
	 * use a viewport {@link #setViewPort(double, double)} which doesn't
	 * displays all data.
	 * 
	 * @param scrollable
	 */
	public void setScrollable(boolean scrollable) {
		this.scrollable = scrollable;
	}

	public void setShowLegend(boolean showLegend) {
		this.showLegend = showLegend;
	}

	/**
	 * set's static vertical labels (from top to bottom)
	 * 
	 * @param verlabels
	 *            if null, labels were generated automatically
	 */
	public void setVerticalLabels(String[] verlabels) {
		this.verlabels = verlabels;
	}
	
	protected void onViewportChanged() {
		this.onViewportChanged(true);	
	}
	
	protected void onViewportChanged(boolean tellChildren) {
		if (tellChildren){
			mContentView.onViewportChanged();
		}
		if (mViewPortListener!=null){
			mViewPortListener.onViewportChanged(viewportStart, viewportSize);
		}
	}

	/**
	 * set's the viewport for the graph.
	 * 
	 * @param start
	 *            x-value
	 * @param size
	 */
	public void setViewPort(double start, double size) {
		viewportStart = start;
		viewportSize = size;
		onViewportChanged();
	}
	public void setViewPortSize(double size){
		if (viewportSize==0){
			moveViewPortStartToBeginning();
		}
		viewportSize = size;
		onViewportChanged();
	}
	public void moveViewPortStartToTheEnd(){
		double newViewPortStart = getMaxX(true) - viewportSize;
		double min =  getMinX(true);
		viewportStart = Math.max(newViewPortStart, min);
		onViewportChanged();
	}
	
	public void moveViewPortStartToBeginning(){
		viewportStart = getMinX(true);
		onViewportChanged();
	}
}
