package com.devbaltasarq.varse.ui.showresult;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;


/** A graph created from lines. */
public class LineChart extends Drawable {
    private static double SCALED_DENSITY;

    /** Builds a new graph, allowing to enter data. */
    public static class Builder {
        public static int[] COLORS = new int[] {
                0xff0000ff,                                 // blue
                0xffff0000,                                 // red
                0xff8b008b,                                 // magenta
                0xff00ff00,                                 // green
                0xff00ced1,                                 // dark turquoise
                0xffffff00,                                 // yellow
                0xff808080,                                 // gray
                0xffffa500,                                 // orange
                0xff9acd32,                                 // yellow-green
                0xff87ceeb,                                 // sky blue
        };

        /** Creates a new builder, ready to get data. */
        public Builder()
        {
            this.lineDataSets = new ArrayList<>();
            this.tagColors = new ArrayList<>();
            this.legendX = this.legendY = "";
        }

        /** Adds a new line data set to the graph. */
        public void add(LineDataSet lineDataSet)
        {
            this.lineDataSets.add( lineDataSet );
        }

        /** Adds a new explanation between association color vs line. */
        public void add(ExplanationColorTag tagColor)
        {
            this.tagColors.add( tagColor );
        }

        /** Sets the legend for the x axis. */
        public void setLegendX(String legendX)
        {
            this.legendX = legendX;
        }

        /** Sets the legend for the y axis. */
        public void setLegendY(String legendY)
        {
            this.legendY = legendY;
        }

        /** @return a new graph, with the up to date data entered. */
        public LineChart build(double scaledDensity)
        {
            final LineChart toret = new LineChart( scaledDensity, this.lineDataSets, this.tagColors );

            // Legends
            toret.setLegendX( this.legendX );
            toret.setLegendY( this.legendY );

            return toret;
        }

        private String legendX;
        private String legendY;
        private ArrayList<LineDataSet> lineDataSets;
        private ArrayList<ExplanationColorTag> tagColors;
    }

    /** Represents the correspondence between color and tag. */
    public static class ExplanationColorTag {
        /** Builds an association between color and tag,
          * for explanatory purposes in the chart.
          * @param tag the tag associated with a color
          * @param color the color this tag is associated to, as an int,
          *              format 0xaarrggbb, a: alpha, r: red, g: green, b: blue,
          *              from 00 to ff.
          */
        public ExplanationColorTag(String tag, int color)
        {
            this.tag = tag;
            this.color = color;
        }

        /** @return the tag associated with a given color. */
        public String getTag()
        {
            return this.tag;
        }

        /** @return the color for this tag. */
        public int getColor()
        {
            return this.color;
        }

        private int color;
        private String tag;
    }

    /** Represents a single point in the graph. */
    public static class Entry {
        public Entry(double x, double y)
        {
            this.x = x;
            this.y = y;
        }

        /** @return the x coordinate. */
        public double getX()
        {
            return this.x;
        }

        /** @return the y coordinate. */
        public double getY()
        {
            return this.y;
        }

        @Override
        public String toString()
        {
            return String.format (Locale.getDefault(),
                    "(%06.2f, %06.2f)",
                    this.getX(), this.getY() );
        }

        private double x;
        private double y;
    }

    /** Represents the data of a given line in the graph. */
    public static class LineDataSet {
        public LineDataSet(Collection<Entry> entries, String tag, int color)
        {
            this.tag = tag;
            this.color = color;
            this.entries = entries.toArray( new Entry[ 0 ] );
        }

        /** @return the length of the data set (number of entries). */
        public int size()
        {
            return this.entries.length;
        }

        /** @return the entry at the given position.
         * @param i the position of the entry to retrieve.
         * @see Entry
         */
        public Entry getEntry(int i)
        {
            return this.entries[ i ];
        }

        /** @return the entries in this data set. */
        public Entry[] getEntries()
        {
            return Arrays.copyOf( this.entries, this.entries.length );
        }

        /** @return the tag for this line. */
        public String getTag()
        {
            return this.tag;
        }

        /** @return the color for this line */
        public int getColor()
        {
            return this.color;
        }

        /** @return all the entries, as a single string. */
        public String entriesAsString()
        {
            final StringBuilder toret = new StringBuilder();
            String delimiter = "";

            for(Entry entry: this.entries) {
                toret.append( delimiter );
                toret.append( entry.toString() );
                delimiter = ", ";
            }

            return toret.toString();
        }

        @Override
        public String toString()
        {
            return String.format( Locale.getDefault(),
                    "{Tag: '%s', Color: %d, Entries: [%s]}",
                    this.getTag(),
                    this.getColor(),
                    this.entriesAsString() );
        }

        private int color;
        private String tag;
        private Entry[] entries;
    }

    /** Constructs a new graph. */
    public LineChart(double scaledDensity,
                     Collection<LineDataSet> lineDataSets,
                     Collection<ExplanationColorTag> colorTags)
    {
        // Set up graphics
        SCALED_DENSITY = scaledDensity;
        this.drawGrid = true;
        this.paint = new Paint();
        this.paint.setStrokeWidth( 2 );

        // Obtain data
        this.lineDataSets = lineDataSets.toArray( new LineDataSet[ 0 ] );
        this.colorTags = colorTags.toArray( new ExplanationColorTag[ 0 ] );

        // Preparation for data normalization
        this.calculateDataMinMax();
    }

    /** Calculates the minimum and maximum values in the data sets.
     * This is stored in the minX, minY, maxX and maxY
     * @see LineDataSet
     */
    private void calculateDataMinMax()
    {
        this.minX = this.minY = Double.MAX_VALUE;
        this.maxX = this.maxY = Double.MIN_VALUE;

        for(LineDataSet lineDataSet: this.lineDataSets) {
            for(Entry entry: lineDataSet.getEntries()) {
                final double X = entry.getX();
                final double Y = entry.getY();

                this.minX = Math.min( this.minX, X );
                this.maxX = Math.max( this.maxX, X );
                this.minY = Math.min( this.minY, Y );
                this.maxY = Math.max( this.maxY, Y );
            }
        }

        return;
    }

    @Override
    public void setAlpha(int x)
    {
    }

    @Override
    public int getOpacity()
    {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter)
    {
    }

    @Override
    protected boolean onLevelChange(int level)
    {
        super.onLevelChange( level );

        this.invalidateSelf();
        return true;
    }

    /** Sets the legend for the x axis. */
    public void setLegendX(String legendX)
    {
        this.legendX = legendX;
    }

    /** Sets the legend for the y axis. */
    public void setLegendY(String legendY)
    {
        this.legendY = legendY;
    }

    /** @return true if the grid will be drawn, false otherwise. */
    public boolean shouldDrawGrid()
    {
        return this.drawGrid;
    }

    /** Changes whether the grid should be drawn or not.
      * @param drawGrid true to draw the grid, false otherwise.
      */
    public void setDrawGrid(boolean drawGrid)
    {
        this.drawGrid = drawGrid;
    }

    @Override
    public void draw(@NonNull Canvas canvas)
    {
        final int LEGEND_SPACE = 50;
        final int CHART_PADDING = 60;
        final int LEGEND_PADDING = 20;
        final float TEXT_SIZE_SP = 10;
        final float TEXT_SIZE = TEXT_SIZE_SP * (float) SCALED_DENSITY;

        // Set up
        this.canvas = canvas;
        this.chartBounds = new Rect( 0,  0, this.canvas.getWidth(), this.canvas.getHeight() );
        this.legendBounds = new Rect( 0, 0, this.canvas.getWidth(), this.canvas.getHeight() );
        this.paint.setTextSize( TEXT_SIZE );

        // Adjust chart bounds
        this.chartBounds.top += CHART_PADDING;
        this.chartBounds.right = ( (int) ( this.getBounds().width() * .75 ) ) - CHART_PADDING;
        this.chartBounds.bottom -= CHART_PADDING + LEGEND_SPACE;
        this.chartBounds.left += CHART_PADDING + LEGEND_SPACE;

        // Adjust legend bounds
        this.legendBounds.top += CHART_PADDING;
        this.legendBounds.right -= LEGEND_PADDING;
        this.legendBounds.bottom -= LEGEND_PADDING;
        this.legendBounds.left = ( (int) ( this.getBounds().width() * .75 ) ) + LEGEND_PADDING;

        // Draw the graph's axis
        this.paint.setStrokeWidth( 6 );
        this.drawAxis();
        this.drawGrid();

        // Draw the data
        this.paint.setStrokeWidth( 4 );
        this.drawData();

        // Draw the legend box
        this.drawLegendBox();
    }

    /** Draws a new line in the canvas.
      * Remember to set the canvas attribute (in LineChart::draw) before using this method.
      * @param x the initial x coordinate.
      * @param y the initial y coordinate.
      * @param x2 the final x coordinate.
      * @param y2 the final y coordinate
      * @param color the color to draw the line with.
      * @see LineChart::draw
      */
    private void line(int x, int y, int x2, int y2, int color)
    {
        this.paint.setColor( color );
        canvas.drawLine( x, y, x2, y2, this.paint );
    }

    /** Writes a real value as text.
     * Remember to set the canvas attribute (in LineChart::draw) before using this method.
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     * @param value the value to show.
     */
    private void write(double x, double y, double value)
    {
        String strNum;

        if ( ( value == Math.floor( value ) )
             && !Double.isInfinite( value ) )
        {
            // It is actually an integer number
            strNum = String.format( Locale.getDefault(), "%02d", (int) value );
        } else {
            strNum = String.format( Locale.getDefault(), "%04.1f", value );
        }

        this.write( x, y, strNum );
    }

    /** Writes a real value as text.
      * Remember to set the canvas attribute (in LineChart::draw) before using this method.
      * @param x the horizontal coordinate
      * @param y the vertical coordinate
      * @param msg the string to show.
      */
    private void write(double x, double y, String msg)
    {
        this.paint.setColor( Color.BLACK );
        this.canvas.drawText( msg, (float) x, (float) y, this.paint );
    }

    /** Draws the grid.
      * @see LineChart::shouldDrawGrid
      */
    private void drawGrid()
    {
        if ( this.shouldDrawGrid() ) {
            final int COLOR = Color.DKGRAY;
            final int NUM_SLOTS = 10;
            final int CHART_RIGHT = this.chartBounds.right;
            final int CHART_LEFT = this.chartBounds.left;
            final int CHART_TOP = this.chartBounds.top;
            final int CHART_BOTTOM = this.chartBounds.bottom;

            this.paint.setStrokeWidth( 1 );

            // Complete rectangle
            this.line( CHART_LEFT, CHART_TOP, CHART_RIGHT, CHART_TOP, COLOR );
            this.line( CHART_RIGHT, CHART_TOP, CHART_RIGHT, CHART_BOTTOM, COLOR );

            // Intermediate vertical lines (marking the x segments)
            final double SLOT_DATA_X = ( this.maxX - this.minX ) / NUM_SLOTS;

            for(int i = 1; i < NUM_SLOTS; ++i) {
                final double DATA_X = this.minX + ( SLOT_DATA_X * i );
                final int X = this.translateX( DATA_X );

                this.write( X - 20, CHART_BOTTOM + 35, DATA_X );
                this.line( X, CHART_BOTTOM, X, CHART_TOP, COLOR );
            }

            // Intermediate horizontal lines (marking the y segments)
            final double SLOT_DATA_Y = ( this.maxY - this.minY ) / NUM_SLOTS;

            for(int i = 1; i < NUM_SLOTS; ++i) {
                final double DATA_Y = this.minY + ( i * SLOT_DATA_Y );
                final int Y = this.translateY( DATA_Y );

                this.write( CHART_LEFT - 60, Y, DATA_Y );
                this.line( CHART_LEFT, Y, CHART_RIGHT, Y, COLOR );
            }
        }

        return;
    }

    /** @return the normalized value for x, right for drawing in the screen. */
    private int translateX(double x)
    {
        final double X = x - this.minX;
        final int NORM_X = (int) ( ( X * this.chartBounds.width() ) / ( this.maxX - this.minX ) );

        return this.chartBounds.left + NORM_X;
    }

    /** @return the normalized value for y, right for drawing in the screen. */
    private int translateY(double y)
    {
        final double Y = y - this.minY;
        final int NORM_Y = (int) ( ( Y * this.chartBounds.height() ) / ( this.maxY - this.minY ) );

        return this.chartBounds.bottom - NORM_Y;
    }

    /** Draws the axis of the graph */
    private void drawAxis()
    {
        final int LEFT = this.chartBounds.left;
        final int BOTTOM = this.chartBounds.bottom;
        final int COLOR = Color.BLACK;

        // Horizontal axis
        this.line( LEFT, BOTTOM, this.chartBounds.right, BOTTOM, COLOR );

        // Vertical axis
        this.line( LEFT, this.chartBounds.top, LEFT, BOTTOM, COLOR );

        // Vertical legend
        float textWidthY = this.paint.measureText( this.legendY );
        int centeredLegendY = ( this.chartBounds.height() / 2 ) - ( (int) ( textWidthY / 2 ) );
        int posLegendYX = LEFT - 70;
        int posLegendYY = BOTTOM - centeredLegendY;
        this.canvas.save();
        this.canvas.rotate( -90, posLegendYX, posLegendYY );
        this.write( posLegendYX, posLegendYY, this.legendY );
        this.canvas.restore();

        // Horizontal legend
        float textWidthX = this.paint.measureText( this.legendX );
        int posLegendX = ( this.chartBounds.width() / 2 ) - ( (int) ( textWidthX / 2 ) );
        this.write( LEFT + posLegendX, BOTTOM + 60, this.legendX );
    }

    /** Draws the data in the chart. */
    private void drawData()
    {
        final Point previousPoint = new Point( -1, -1 );

        for(LineDataSet lineDataSet: this.lineDataSets) {
            for(Entry entry: lineDataSet.getEntries()) {
                final int X = this.translateX( entry.getX() );
                final int Y = this.translateY( entry.getY() );

                if ( previousPoint.x >= 0 ) {
                    this.line(  previousPoint.x, previousPoint.y, X, Y, lineDataSet.getColor() );

                    this.write( X + 10, Y - 10, entry.getY() );
                }

                previousPoint.x = X;
                previousPoint.y = Y;
            }
        }

        return;
    }

    /** Draws the box with all legends. */
    private void drawLegendBox()
    {
        final double LETTER_WIDTH = this.paint.measureText( "W" );
        final int MAX_LENGTH = (int) ( this.legendBounds.width() / LETTER_WIDTH );

        int y = this.legendBounds.top;

        for(ExplanationColorTag colorTag: this.colorTags) {
            String tag = colorTag.getTag();
            final double TEXT_WIDTH = this.paint.measureText( tag );


            if ( TEXT_WIDTH > this.legendBounds.width() ) {
                tag = tag.substring( 0, MAX_LENGTH ) + "...";
            }

            this.paint.setColor( colorTag.getColor() );
            this.paint.setStyle( Paint.Style.FILL_AND_STROKE);
            this.canvas.drawRect(
                                this.legendBounds.left,
                                y,
                                this.legendBounds.left + 20,
                                y - 20,
                                this.paint );

            this.paint.setStyle( Paint.Style.STROKE );
            this.paint.setColor( Color.BLACK );
            this.write( this.legendBounds.left + 50, y, tag );

            y += 50;
        }

        return;
    }

    private String legendX;
    private String legendY;
    private boolean drawGrid;
    private Rect chartBounds;
    private Rect legendBounds;
    private Paint paint;
    private Canvas canvas;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private LineDataSet[] lineDataSets;
    private ExplanationColorTag[] colorTags;
}
