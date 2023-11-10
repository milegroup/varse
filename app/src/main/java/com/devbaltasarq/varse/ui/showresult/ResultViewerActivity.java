// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.ui.showresult;


import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
    private static final int MAX_TAGS = 5;


    @Override
    @SuppressWarnings({"ClickableViewAccessibility", "deprecation"})
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_result_viewer );

        // Back button
        final ImageButton BT_BACK = this.findViewById( R.id.btCloseResultViewer );
        BT_BACK.setOnClickListener( (v) -> this.finish() );

        // Change data/graphic button
        final ImageButton BT_GRAPH = this.findViewById( R.id.btGraphic );
        final ImageButton BT_DATA = this.findViewById( R.id.btData );

        BT_GRAPH.setOnClickListener( (v) -> this.changeToGraphView() );
        BT_DATA.setOnClickListener( (v) -> this.changeToDataView() );

        // Chart image viewer
        final StandardGestures GESTURES = new StandardGestures( this );
        this.chartView = findViewById( R.id.ivChartViewer );
        this.chartView.setOnTouchListener( GESTURES );

        final TextView BOX_DATA = this.findViewById( R.id.lblTextData );
        BOX_DATA.setMovementMethod( new ScrollingMovementMethod() );

        this.resultAnalyzer = new ResultAnalyzer( result, MAX_TAGS );
        this.resultAnalyzer.analyze();

        if ( this.resultAnalyzer.getRRnfCount() > 0  ) {
            // Plots interpolated HR signal and data analysis
            this.plotChart();
            final String REPORT = this.resultAnalyzer.buildReport();

            if ( android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ) {
                BOX_DATA.setText( Html.fromHtml( REPORT, Html.FROM_HTML_MODE_COMPACT ) );
            } else {
                BOX_DATA.setText( Html.fromHtml( REPORT ) );
            }
        } else {
            this.showStatus( LogTag, "Empty data" );
        }

        this.changeToGraphView();
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    /** Show the graphic and hide the data info. */
    private void changeToGraphView()
    {
        final ImageButton BT_GRAPH = this.findViewById( R.id.btGraphic );
        final ImageButton BT_DATA = this.findViewById( R.id.btData );
        final ImageView IV_CHART = this.findViewById( R.id.ivChartViewer );
        final TextView TV_DATA = this.findViewById( R.id.lblTextData );

        BT_DATA.getDrawable().mutate().setTint( 0xff000000 );
        BT_DATA.setEnabled( true );
        BT_GRAPH.getDrawable().mutate().setTint( 0xffaaaaaa );
        BT_GRAPH.setEnabled( false );

        TV_DATA.setVisibility( View.GONE );
        IV_CHART.setVisibility( View.VISIBLE );
    }

    /** Show the text view and hide the graphic. */
    private void changeToDataView()
    {
        final ImageButton BT_GRAPH = this.findViewById( R.id.btGraphic );
        final ImageButton BT_DATA = this.findViewById( R.id.btData );
        final ImageView IV_CHART = this.findViewById( R.id.ivChartViewer );
        final TextView TV_DATA = this.findViewById( R.id.lblTextData );

        BT_GRAPH.getDrawable().mutate().setTint( 0xff000000 );
        BT_GRAPH.setEnabled( true );
        BT_DATA.getDrawable().mutate().setTint( 0xffaaaaaa );
        BT_DATA.setEnabled( false );

        IV_CHART.setVisibility( View.GONE );
        TV_DATA.setVisibility( View.VISIBLE );
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
        String toret = this.getString( R.string.lblDefaultTag );

        if ( timeValue < EPISODE_INITS[ 0 ] ) {
            toret = EPISODE_TYPES[ 0 ];
        }
        else
        if ( timeValue > EPISODE_ENDS[ EPISODE_ENDS.length - 1 ] ) {
            toret = EPISODE_TYPES[ EPISODE_TYPES.length - 1 ];
        } else {
            for (int i = 0; i < EPISODE_TYPES.length; ++i) {
                if ( ( timeValue >= EPISODE_INITS[ i ] )
                  && ( timeValue <= EPISODE_ENDS[ i ] ) )
                {
                    toret = EPISODE_TYPES[ i ];
                    break;
                }
            }
        }

        return toret;
    }

    private double getDensity()
    {
        double toret = 0.0;

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ) {
            toret = this.getWindow().getWindowManager().getCurrentWindowMetrics().getDensity();
        } else {
            toret = this.getResources().getDisplayMetrics().scaledDensity;
        }

        return toret;
    }

    /** Plots the chart in a drawable and shows it. */
    private void plotChart()
    {
        final ArrayList<LineChart.SeriesInfo> SERIES = new ArrayList<>();
        final ArrayList<LineChart.Point> POINTS = new ArrayList<>();
        final Float[] DATA_HR_INTERP_X = this.resultAnalyzer.getDataHRInterpolatedForX();
        final Float[] DATA_HR_INTERP = this.resultAnalyzer.getDataHRInterpolated();
        final double DENSITY = this.getDensity();

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
    private ResultAnalyzer resultAnalyzer;
    public static Result result;

    /** Manages gestures. */
    public static class StandardGestures implements View.OnTouchListener,
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
        public boolean onScaleBegin(@NonNull  ScaleGestureDetector detector)
        {
            return true;
        }

        @Override
        public void onScaleEnd(@NonNull ScaleGestureDetector detector)
        {
        }

        private final PointF position;
        private View view;
        private final ScaleGestureDetector gestureScale;
        private float scaleFactor = 1;
    }
}
