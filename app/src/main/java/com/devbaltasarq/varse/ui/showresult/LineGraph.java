package com.devbaltasarq.varse.ui.showresult;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;


/** A graph created from lines. */
public class LineGraph extends Drawable {
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
                                        "(%06.2f, 06.2f)",
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
              * @return
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

        /** Creates a new builder, ready to get data. */
        public Builder()
        {
            this.lineDataSets = new ArrayList<>();
        }

        /** Adds a new line data set to the graph. */
        public void add(LineDataSet lineDataSet)
        {
            this.lineDataSets.add( lineDataSet );
        }

        /** @return a new graph, with the up to date data entered. */
        public LineGraph build()
        {
            final LineGraph toret = new LineGraph();

            return toret;
        }

        private ArrayList<LineDataSet> lineDataSets;
    }

    /** Constructs a new graph. */
    public LineGraph()
    {
        this.paint = new Paint();
        this.paint.setStrokeWidth( 2 );
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

    @Override
    public void draw(Canvas canvas)
    {
        // Set up
        this.bounds = this.getBounds();
        this.canvas = canvas;

        // Adjust bounds
        this.bounds.top += 50;
        this.bounds.right -= 50;
        this.bounds.bottom -= 50;
        this.bounds.left += 50;

        // Draw the graph
        this.paint.setStrokeWidth( 4 );
        this.drawAxis();

        // Draw the data
        this.paint.setStrokeWidth( 2 );
    }

    /** Draws a new line in the canvas. Rember to set the canvas attribute.
      * @param x the initial x coordinate.
      * @param y the initial y coordinate.
      * @param x2 the final x coordinate.
      * @param y2 the final y coordinate
      * @param color the color to draw the line with.
      */
    private void line(int x, int y, int x2, int y2, int color)
    {
        this.paint.setColor( color );
        canvas.drawLine( x, y, x2, y2, this.paint );
    }

    /** Draws the axis of the graph */
    private void drawAxis()
    {
        // Horizontal axis
        this.line( this.bounds.left, this.bounds.height(),
                   this.bounds.width(), this.bounds.height(),
                   0xff000000 );

        // Vertical axis
        this.line(  this.bounds.left, this.bounds.top,
                    this.bounds.left, this.bounds.height(),
                    0xff000000 );
    }

    private Rect bounds;
    private Paint paint;
    private Canvas canvas;
}
