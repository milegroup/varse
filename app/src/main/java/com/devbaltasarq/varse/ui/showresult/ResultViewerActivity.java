// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.ui.showresult;


import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.ui.AppActivity;
import com.devbaltasarq.varse.core.ResultAnalyzer;

import java.util.ArrayList;


/** Represents the result data set as a graph on the screen.
  * @author Leandro (removed chart dependency and temporary file loading by baltasarq)
  */
public class ResultViewerActivity extends AppActivity {
    private static final String LogTag = ResultViewerActivity.class.getSimpleName();
    private static int MAX_TAGS = 5;


    @Override @SuppressWarnings("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_result_viewer );

        // Back button
        final ImageButton BT_BACK = this.findViewById( R.id.btCloseResultViewer );
        BT_BACK.setOnClickListener( (v) -> this.finish() );

        // Chart image viewer
        final StandardGestures GESTURES = new StandardGestures( this );
        this.chartView = findViewById( R.id.ivChartViewer );
        this.chartView.setOnTouchListener( GESTURES );
        this.boxdata = this.findViewById( R.id.lblTextData );
        this.boxdata.setMovementMethod( new ScrollingMovementMethod() );

        this.resultAnalyzer = new ResultAnalyzer( result, MAX_TAGS );
        this.resultAnalyzer.analyze();

        if ( this.resultAnalyzer.getRRnfCount() > 0  ) {
            // Plots interpolated HR signal and data analysis
            this.plotChart();
            final String REPORT = this.resultAnalyzer.buildReport();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                this.boxdata.setText( Html.fromHtml( REPORT, Html.FROM_HTML_MODE_COMPACT ) );
            } else {
                this.boxdata.setText( Html.fromHtml( REPORT ) );
            }
        } else {
            this.showStatus( LogTag, "Empty data" );
        }

        return;
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    /** Sets the color of the data line by tag or the color index.
      * If the tag is none, then the color must be black.
      * If the color index is greater than the MAX_TAGS or the array of colors,
      * then it will be also black.
      * @param tag The tag, to compare to NONE_TAG.
      * @param colorIndex The current color index.
      */
    private int calculateColor(String tag, int colorIndex)
    {
        final int[] COLORS = LineChart.COLORS;
        int toret = Color.BLACK;

        if ( !tag.isEmpty()
          && colorIndex < MAX_TAGS
          && colorIndex < COLORS.length )
        {
            toret = COLORS[ colorIndex ];
        }

        return toret;
    }

    /** @return the tag corresponding to the given time value. */
    private String findTag(float timeValue)
    {
        final String[] EPISODE_TYPES = this.resultAnalyzer.getEpisodeTypes();
        final Float[] EPISODE_INITS = this.resultAnalyzer.getEpisodeInits();
        final Float[] EPISODE_ENDS = this.resultAnalyzer.getEpisodeEnds();
        String tag = this.getString( R.string.lblDefaultTag );

        for (int i = 0; i < EPISODE_TYPES.length; i++) {
            if ( ( EPISODE_INITS[ i ] <= timeValue )
                    && ( EPISODE_ENDS[ i ] >= timeValue ) )
            {
                tag = EPISODE_TYPES[ i ];
                break;
            }
        }

        return tag;
    }

    /** Plots the chart in a drawable and shows it. */
    private void plotChart()
    {
        final double DENSITY = this.getResources().getDisplayMetrics().scaledDensity;
        final ArrayList<LineChart.SeriesInfo> SERIES = new ArrayList<>();
        final ArrayList<LineChart.Point> POINTS = new ArrayList<>();
        final Float[] DATA_HR_INTERP_X = this.resultAnalyzer.getDataHRInterpolatedForX();
        final Float[] DATA_HR_INTERP = this.resultAnalyzer.getDataHRInterpolated();

        if ( DATA_HR_INTERP_X.length > 0 ) {
            String tag = this.findTag( DATA_HR_INTERP_X[ 0 ] );
            int color = this.calculateColor( tag, 0 );
            int index = 0;
            int colorIndex = 0;

            SERIES.add( new LineChart.SeriesInfo( tag, color ) );

            for(float time: DATA_HR_INTERP_X ) {
                final double BPM = DATA_HR_INTERP[ index ];
                final String NEW_TAG = this.findTag( time );

                if ( !tag.equals( NEW_TAG ) ) {
                    ++colorIndex;
                    tag = NEW_TAG;
                    color = this.calculateColor( NEW_TAG, colorIndex );
                    SERIES.add( new LineChart.SeriesInfo( NEW_TAG, color ) );
                }

                POINTS.add( new LineChart.Point( time, BPM, color ) );
                ++index;
            }
        }

        // If there are no series, then there is solely the black/default series.
        if ( SERIES.size() == 0 ) {
            SERIES.add( new LineChart.SeriesInfo(
                                this.getString( R.string.lblDefaultTag ),
                                Color.BLACK ) );
        }

        // Now create and draw it.
        final LineChart CHART = new LineChart( DENSITY, POINTS, SERIES );
        CHART.setLegendY( "Heart-rate (bpm)" );
        CHART.setLegendX( "Time (sec.)" );
        CHART.setShowLabels( false );
        this.chartView.setScaleType( ImageView.ScaleType.MATRIX );
        this.chartView.setImageDrawable( CHART );
    }

    private ImageView chartView;
    private TextView boxdata;
    private ResultAnalyzer resultAnalyzer;
    public static Result result;

    /** Manages gestures. */
    public class StandardGestures implements View.OnTouchListener,
            ScaleGestureDetector.OnScaleGestureListener
    {

        public StandardGestures(Context c)
        {
            this.gestureScale = new ScaleGestureDetector( c, this );
            this.position = new PointF( 0, 0);
        }

        @Override @SuppressWarnings("ClickableViewAccessibility")
        public boolean onTouch(View view, MotionEvent event)
        {
            float curX;
            float curY;

            this.view = view;
            this.gestureScale.onTouchEvent( event );

            if ( !this.gestureScale.isInProgress() ) {
                switch ( event.getAction() ) {
                    case MotionEvent.ACTION_DOWN:
                        this.position.x = event.getX();
                        this.position.y = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        curX = event.getX();
                        curY = event.getY();
                        this.view.scrollBy( (int) ( this.position.x - curX ), (int) ( this.position.y - curY ) );
                        this.position.x = curX;
                        this.position.y = curY;
                        break;
                    case MotionEvent.ACTION_UP:
                        curX = event.getX();
                        curY = event.getY();
                        this.view.scrollBy( (int) ( this.position.x - curX ), (int) ( this.position.y - curY ) );
                        break;
                }
            }

            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            this.scaleFactor *= detector.getScaleFactor();

            // Prevent view from becoming too small
            this.scaleFactor = ( this.scaleFactor < 1 ? 1 : this.scaleFactor );

            // Change precision to help with jitter when user just rests their fingers
            this.scaleFactor = ( (float) ( (int) ( this.scaleFactor * 100 ) ) ) / 100;
            this.view.setScaleX( this.scaleFactor );
            this.view.setScaleY( this.scaleFactor) ;

            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector)
        {
        }

        private PointF position;
        private View view;
        private ScaleGestureDetector gestureScale;
        private float scaleFactor = 1;
    }
}
