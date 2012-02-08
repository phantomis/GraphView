package com.jjoe64.graphs;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries.GraphViewData;

/**
 * Draws a Bar Chart
 * @author Muhammad Shahab Hameed
 */
public class BarGraphView extends GraphView {
	public BarGraphView(Context context, String title) {
		super(context);
	}

	@Override
	public void drawSeries(Canvas canvas,int color,  List<GraphViewData> values, float graphwidth, float graphheight,
			float border, double minX, double minY, double diffX, double diffY,
			float horstart) {
		float colwidth = (graphwidth - (2 * border)) / values.size();

		// draw data
		for (int i = 0; i < values.size(); i++) {
			float valY = (float) (values.get(i).valueY - minY);
			float ratY = (float) (valY / diffY);
			float y = graphheight * ratY;
			canvas.drawRect((i * colwidth) + horstart, (border - y) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), graphheight + border - 1, paint);
		}
	}
}
