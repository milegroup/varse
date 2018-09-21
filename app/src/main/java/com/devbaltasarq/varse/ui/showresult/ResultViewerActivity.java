package com.devbaltasarq.varse.ui.showresult;

import android.os.Bundle;
import android.graphics.Color;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.ui.AppActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Represents the result data set as a graph on the screen.
  * public static File beatsFile y tagsFile deben ser cargadas primero.
  * @author Leandro
  */
public class ResultViewerActivity extends AppActivity {
    private static final String LogTag = ResultViewerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_result_viewer );

        final ImageButton btCloseResultViewer = this.findViewById( R.id.btCloseResultViewer );

        btCloseResultViewer.setOnClickListener( (v) -> this.finish() );

        // Check files exist
        if ( tagsFile == null
          || !tagsFile.exists() )
        {
            this.showStatus( LogTag, "Tags file not found." );
            return;
        }

        if ( beatsFile == null
          || !beatsFile.exists() )
        {
            this.showStatus( LogTag, "Heart beats file not found." );
            return;
        }

        chart = findViewById(R.id.chart);
        boxdata = findViewById(R.id.textdata);
        boxdata.setMovementMethod(new ScrollingMovementMethod());

        // Loads data into dataRRnf (unfiltered RR in milliseconds)
        dataRRnf = new ArrayList<>();
        episodesInits = new ArrayList<>();
        episodesEnds = new ArrayList<>();
        episodesType = new ArrayList<>();
        LoadData( beatsFile );
        LoadTags( tagsFile );

        if ( dataRRnf.size() > 0 ) {
            // Generates dataHRnf (unfiltered sequence of BPS values)
            dataHRnf = new ArrayList<>();
            for (int i = 0; i< dataRRnf.size(); i++) {
                dataHRnf.add(60.0f/(dataRRnf.get(i)/1000.0f));
            }

            // Calculates dataBeatTimesnf (unfiltered beat positions in seconds) from dataRRnf
            dataBeatTimesnf = new ArrayList<>();
            dataBeatTimesnf.add(dataRRnf.get(0)/1000.0f);
            for (int i=1; i<dataRRnf.size(); i++) {
                dataBeatTimesnf.add(dataBeatTimesnf.get(i-1)+dataRRnf.get(i)/1000.0f);
            }

            // Filters beat times creating a sequence of RR intervals
            dataBeatTimes = new ArrayList<>(dataBeatTimesnf);
            dataHR = new ArrayList<>(dataHRnf);
            dataRR = new ArrayList<>(dataRRnf);
            FilterData();


            Log.i(LogTag,"Filtered sequence: "+dataBeatTimes.size()+" values");
            Log.i(LogTag,"Last beat position: "+dataBeatTimes.get(dataBeatTimes.size()-1)+" seconds");

            // Creates a series of HR values linearly interpolated
            xInterp = new ArrayList<>();
            dataHRInterp = new ArrayList<>();
            this.Interpolate();

            Log.i(LogTag,"length of xinterp: "+xInterp.size());
            Log.i(LogTag,"First value: "+xInterp.get(0));
            Log.i(LogTag,"Last value: "+xInterp.get(xInterp.size()-1));

            // Plots interpolated HR signal
            this.makePlot();
            this.Analyze();
        } else {
            this.showStatus( LogTag, "Empty data" );
        }
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    private void LoadData(File f)
    {
        // Loads the data

        try {
            Log.i(LogTag,"Loading data");
            BufferedReader reader = Orm.openReaderFor( f );
            String line;
            float fData;

            while ((line = reader.readLine()) != null) {
                fData = Float.parseFloat(line);
                dataRRnf.add(fData);

            }
            Orm.close( reader );
            Log.i(LogTag,"Size of vector: " + dataRRnf.size());

        } catch (IOException exc) {
            this.showStatus( LogTag, "unable to load data file: " + f.getName() );
        }
    }

    private void LoadTags(File f) {
        // Loads episodic information
        Log.i(LogTag + ".Tags","Loading tags");
        int numLines = 0;

        try {
            BufferedReader reader = Orm.openReaderFor( f );
            String line;

            while ((line = reader.readLine()) != null) {
                if (numLines != 0) { // Ignores first line
                    Log.i(LogTag + ".Tags",line);
                    String[] parts = line.trim().split(" +|\t");
                    String[] hms = parts[0].split(":");

                    try {
                        Float init = 3600.0f * Float.parseFloat(hms[0]) + 60.0f*Float.parseFloat(hms[1]) + Float.parseFloat(hms[2]);
                        Float end = init + Float.parseFloat(parts[2]);


                        Log.i(LogTag + ".Tags", "Tag: "+parts[1]);
                        Log.i(LogTag + ".Tags", "Init: "+parts[0]+" ("+init+" seconds)");
                        Log.i(LogTag + ".Tags", "End: "+end+" seconds");

                        // ONly now create the episode time marks
                        episodesType.add(parts[1]);
                        episodesInits.add(init);
                        episodesEnds.add(end);
                    } catch(NumberFormatException exc) {
                        Log.e( LogTag, "floating point conversion error: " + exc.getMessage() );
                        return;
                    }

                    numOfTags++;
                }

                numLines++;
            }

            Orm.close( reader );
        } catch (IOException exc) {
            this.showStatus( LogTag, "Error loading tags file: " + f.getName() );
        }

        Log.i(LogTag,"Number of tags: "+numOfTags);

    }

    private String findTag(float timeValue) {
        // Given a time value, it returns the tag
        String tag="None";

        for (int i=0; i<episodesType.size();i++) {
            if ((episodesInits.get(i)<=timeValue) && (episodesEnds.get(i)>=timeValue)) {
                tag = episodesType.get(i);
                break;
            }
        }

        return tag;
    }

    private List<Float> getSegmentRR(int tagIndex) {
        List<Float> segment = new ArrayList<>();
        Log.i(LogTag,"Getting segment corresponding to tag: "+episodesType.get(tagIndex));
        for (int indexRR=0; indexRR < dataRR.size(); indexRR++) {
            if ((dataBeatTimes.get(indexRR)>=episodesInits.get(tagIndex)) && (dataBeatTimes.get(indexRR)<=episodesEnds.get(tagIndex))) {
                segment.add(dataRR.get(indexRR));
            }
        }
        Log.i(LogTag,"Num of values: "+segment.size());

        return segment;
    }

    private void FilterData() {
        int winlength = 50, minbpm=24, maxbpm=198;
        float last = 13.0f;
        dataRR = new ArrayList<>(dataRRnf);
        Log.i(LogTag,"I'm going to filter the signal");

        float ulast = last;
        float umean = 1.5f * ulast;
        int index;
        index = 1;
        while (index < dataHR.size() -1) {
            List<Float> v = dataHR.subList(Math.max(index-winlength,0),index);

            float M = 0.0f;  // M = mean(v)
            for (int i=0 ; i < v.size() ; i++) {
                M += v.get(i);
            }
            M = M/v.size();

            if ( ( (100*Math.abs((dataHR.get(index)- dataHR.get(index-1))/ dataHR.get(index-1)) < ulast) ||
                    (100*Math.abs((dataHR.get(index)- dataHR.get(index+1))/ dataHR.get(index+1)) < ulast) ||
                    (100*Math.abs((dataHR.get(index)-M)/M) < umean) )
                    && (dataHR.get(index) > minbpm) && (dataHR.get(index) < maxbpm)) {
                index += 1;
            } else {
//                Log.i(LogTag,"Removing beat index "+index);
                dataHR.remove(index);
                dataBeatTimes.remove(index);
                dataRR.remove(index);
            }
        }

    }

    private void Interpolate() {
        float xmin = dataBeatTimes.get(0);
        float xmax = dataBeatTimes.get(dataBeatTimes.size()-1);
        float step = 1.0f/freq;

        // Calculates positions in x axis
        xInterp.add(xmin);
        float newValue = xmin+step;
        while (newValue<=xmax) {
            xInterp.add(newValue);
            newValue += step;
        }

        int leftHRIndex, rightHRIndex;
        float leftBeatPos, rightBeatPos, leftHRVal, rightHRVal;
        leftHRIndex = 0;
        rightHRIndex = 1;
        leftBeatPos = dataBeatTimes.get(leftHRIndex);
        rightBeatPos = dataBeatTimes.get(rightHRIndex);
        leftHRVal = dataHR.get(leftHRIndex);
        rightHRVal = dataHR.get(rightHRIndex);

        for (int xInterpIndex=0; xInterpIndex<xInterp.size(); xInterpIndex++ ){
            if (xInterp.get(xInterpIndex)>=rightBeatPos) {
                leftHRIndex++;
                rightHRIndex++;
                leftBeatPos = dataBeatTimes.get(leftHRIndex);
                rightBeatPos = dataBeatTimes.get(rightHRIndex);
                leftHRVal = dataHR.get(leftHRIndex);
                rightHRVal = dataHR.get(rightHRIndex);
            }

            // Estimate HR value in position
            float HR = (rightHRVal-leftHRVal)*(xInterp.get(xInterpIndex)-leftBeatPos)/(rightBeatPos-leftBeatPos)+leftHRVal;
            dataHRInterp.add(HR);

        }

    }

    private List<Float> getSegmentHRInterp(float beg, float end) {
        List<Float> segment = new ArrayList<>();
        for (int indexHR=0 ; indexHR<dataHRInterp.size() ; indexHR++) {
            if  ( (xInterp.get(indexHR) >= beg) && (xInterp.get(indexHR) <= end) ) {
                segment.add(dataHRInterp.get(indexHR));
            }
        }
        return segment;
    }


    private double[] padSegmentHRInterp(List<Float> hrSegment, int newLength) {
        double[] segmentPadded = new double[newLength];
        for (int index = 0 ; index < hrSegment.size() ; index++) {
            segmentPadded[index] =  (double) (hrSegment.get(index));
        }
        return segmentPadded;
    }



    private void Analyze() {

        String text="<h3>Signal data</h3>";
        text += "<p>&nbsp;&nbsp;<b>Length of original RR signal</b>: " + String.format("%d", dataRRnf.size()) + " values</p>";
        text += "<p>&nbsp;&nbsp;<b>Length of filtered RR signal</b>: " + String.format("%d", dataRR.size()) + " values</p>";
        float filteredRate = 100.0f * (dataRRnf.size() - dataRR.size()) / dataRRnf.size();
        text += "<p>&nbsp;&nbsp;<b>Beat rejection rate</b>: " + String.format("%.2f", filteredRate) + "%</p>";
        text += "<p>&nbsp;&nbsp;<b>Interpolation frequency</b>: " + String.format("%.2f", freq) + " Hz</p>";
        text += "<p>&nbsp;&nbsp;<b>Number of interpolated samples</b>: " + String.format("%d", dataHRInterp.size()) +"</p>";


        // ------------------------

        text += "<br/><h3>HRV time-domain results</h3>";

        text += "<p>&nbsp;&nbsp;<b>Mean RR (AVNN)</b>: " + String.format("%.2f", calculateMean(dataRR)) + " ms</p>";
        text += "<p>&nbsp;&nbsp;<b>STD RR (SDNN)</b>: " + String.format("%.2f", calculateSTD(dataRR)) + " ms</p>";
        text += "<p>&nbsp;&nbsp;<b>pNN50</b>: " + String.format("%.2f", calculatePNN50(dataRR)) + "%</p>";
        text += "<p>&nbsp;&nbsp;<b>rMSSD</b>: " + String.format("%.2f", calculateRMSSD(dataRR)) + " ms</p>";
        text += "<p>&nbsp;&nbsp;<b>normHRV</b>: " + String.format("%.2f", calculateNormHRV(dataRR)) + "</p>";

        // ------------------------

        text += "<br/><h3>HRV frequency-domain results</h3>";

        calculateSpectrum(dataHRInterp);

        // ------------------------

        for (int tagIndex = 0; tagIndex < numOfTags; tagIndex++) {
            text += "<br/><h3> Tag: "+episodesType.get(tagIndex)+"</h3>";
            float length = episodesEnds.get(tagIndex)-episodesInits.get(tagIndex);
            text += "<p>&nbsp;&nbsp;<b>Length</b>: " + String.format("%.2f", length) + " s</p>";

            List<Float> tagSegment = getSegmentRR(tagIndex);
            text += "<p>&nbsp;&nbsp;<b>Mean RR (AVNN)</b>: " + String.format("%.2f", calculateMean(tagSegment)) + " ms</p>";
            text += "<p>&nbsp;&nbsp;<b>STD RR (SDNN)</b>: " + String.format("%.2f", calculateSTD(tagSegment)) + " ms</p>";
            text += "<p>&nbsp;&nbsp;<b>pNN50</b>: " + String.format("%.2f", calculatePNN50(tagSegment)) + "%</p>";
            text += "<p>&nbsp;&nbsp;<b>rMSSD</b>: " + String.format("%.2f", calculateRMSSD(tagSegment)) + " ms</p>";
            text += "<p>&nbsp;&nbsp;<b>normHRV</b>: " + String.format("%.2f", calculateNormHRV(tagSegment)) + "</p>";

        }



        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            boxdata.setText(Html.fromHtml(text,Html.FROM_HTML_MODE_COMPACT));
        } else {
            boxdata.setText(Html.fromHtml(text));
        }
    }


    private float calculateMean(List<Float> signal) {
        float sum = 0.0f;
        for (int i=0 ; i < signal.size() ; i++) {
            sum += signal.get(i);
        }
        return sum/signal.size();
    }

    private float calculateSTD(List<Float> signal) {
        float std = 0.0f;
        for (int i=1 ; i < signal.size() ; i++) {
            std += Math.pow(signal.get(i)-calculateMean(signal),2);
        }
        std /= (signal.size()-1);
        std = (float) Math.sqrt(std);
        return std;
    }

    private float calculateRMSSD(List<Float> signal) {
        float rrdifs2 = 0.0f;
        for (int i=1 ; i < signal.size() ; i++) {
            rrdifs2 += Math.pow((signal.get(i) - signal.get(i-1)),2);
        }
        return (float) Math.sqrt(rrdifs2/(signal.size()-1));
    }

    private float calculateNormHRV(List<Float> signal) {
        float lnrMSSD = (float) Math.log(calculateRMSSD(signal));
        return lnrMSSD*100.0f/6.5f;
    }

    private float calculatePNN50(List<Float> signal) {
        int numIntervals = 0;
        int numBigIntervals = 0;
        for (int i=1 ; i < signal.size() ; i++) {
            numIntervals++;
            if (Math.abs(signal.get(i)-signal.get(i-1)) > 50.0) {
                numBigIntervals++;
            }
        }
        return 100.0f * ((float)numBigIntervals/(float)numIntervals);
    }

    private void calculateSpectrum(List<Float> dataHRInterp) {
        Log.i(LogTag + ".SpecXX","Calculating spectrum");
        Log.i(LogTag + ".SpecXX","Number of samples: "+dataHRInterp.size());
        Log.i(LogTag + ".SpecXX","Minimum time: "+xInterp.get(0)+" seconds");
        Log.i(LogTag + ".SpecXX","Maximum time: "+xInterp.get(xInterp.size()-1)+" seconds");
        float analysisWindowLength = ( (xInterp.get(xInterp.size()-1))-xInterp.get(0) ) / 3.0f;
        Log.i(LogTag + ".SpecXX","Analysis window length: "+ analysisWindowLength +" seconds");

        // Five windows, length 1/3 of signal, overlap 50%

        float beg[] = new float[5];
        float end[] = new float[5];

        beg[0] = xInterp.get(0);
        end[0] = beg[0] + analysisWindowLength;

        for (int index=1 ; index < 5; index++ ) {
            beg[index] = beg[index-1] + analysisWindowLength / 2.0f;
            end[index] = beg[index] + analysisWindowLength;
        }

        for (int index=0; index < 5; index++) {
            Log.i(LogTag + ".SpecXX","Window number "+ (index+1) +": ("+ beg[index] + "," + end[index] +") seconds");
        }

        int maxSegmentLength = 0;
        for (int index=0; index<5; index++) {
            List<Float> segmentTMP = new ArrayList<>();
            segmentTMP = getSegmentHRInterp(beg[index],end[index]);
            if ( segmentTMP.size() > maxSegmentLength )
                maxSegmentLength = segmentTMP.size();
        }

        int paddedLength = (int) Math.pow(2,(int) Math.ceil(Math.log((double) maxSegmentLength) / Math.log(2.0)));

        Log.i(LogTag + ".SpecXX","Max segment length: "+maxSegmentLength);
        Log.i(LogTag + ".SpecXX","Padded length: "+paddedLength);

        List<Float> HRSegment = getSegmentHRInterp(beg[0],end[0]);
        Log.i(LogTag + ".SpecXX", "Segment 0 - number of samples: "+HRSegment.size());
        double avg = 0.0;
        for  (int index=0 ; index < HRSegment.size() ; index++) {
            avg += HRSegment.get(index);
        }
        avg = avg / HRSegment.size();
        for  (int index=0 ; index < HRSegment.size() ; index++) {
            HRSegment.set( index , (float) (HRSegment.get(index)-avg) );
        }

        double[] hamWindow = makeHammingWindow(HRSegment.size());
        for (int index=0 ; index < HRSegment.size() ; index++) {
            HRSegment.set(index, (float) (HRSegment.get(index)*hamWindow[index]));
        }

        // writeFile("timeSignal.txt", HRSegment);

        double[] HRSegmentPaddedX = padSegmentHRInterp(HRSegment, paddedLength);
        Log.i(LogTag + ".SpecXX", "Length of padded array: "+HRSegmentPaddedX.length);

        List<Double> HRSPtmp = new ArrayList<>();
        for (int index=0 ; index < HRSegmentPaddedX.length ; index++) {
            HRSPtmp.add(HRSegmentPaddedX[index]);
        }
        // writeFile("timeSignalPadded.txt", HRSPtmp);
        double[] HRSegmentPaddedY = new double[HRSegmentPaddedX.length];

        fft(HRSegmentPaddedX,HRSegmentPaddedY, HRSegmentPaddedX.length);

        List<Float> Spectrum = new ArrayList<>();
        for (int index=0 ; index<HRSegmentPaddedX.length ; index++) {
            Spectrum.add((float) (Math.sqrt( Math.pow(HRSegmentPaddedX[index],2)+Math.pow(HRSegmentPaddedY[index],2) )));
        }

        // writeFile("spectrum.txt", Spectrum);
    }


    public void fft(double[] x, double[] y, int n)
    {
        int i,j,k,n1,n2,a;
        double c,s,e,t1,t2;

        int m = (int)(Math.log(n) / Math.log(2));

        double[] cos = new double[n/2];
        double[] sin = new double[n/2];
        for(int index=0; index<n/2; index++) {
            cos[index] = Math.cos(-2*Math.PI*index/n);
            sin[index] = Math.sin(-2*Math.PI*index/n);
        }


        // Bit-reverse
        j = 0;
        n2 = n/2;
        for (i=1; i < n - 1; i++) {
            n1 = n2;
            while ( j >= n1 ) {
                j = j - n1;
                n1 = n1/2;
            }
            j = j + n1;

            if (i < j) {
                t1 = x[i];
                x[i] = x[j];
                x[j] = t1;
                t1 = y[i];
                y[i] = y[j];
                y[j] = t1;
            }
        }

        // FFT
        n1 = 0;
        n2 = 1;

        for (i=0; i < m; i++) {
            n1 = n2;
            n2 = n2 + n2;
            a = 0;

            for (j=0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a +=  1 << (m-i-1);

                for (k=j; k < n; k=k+n2) {
                    t1 = c*x[k+n1] - s*y[k+n1];
                    t2 = s*x[k+n1] + c*y[k+n1];
                    x[k+n1] = x[k] - t1;
                    y[k+n1] = y[k] - t2;
                    x[k] = x[k] + t1;
                    y[k] = y[k] + t2;
                }
            }
        }
    }

    protected double[] makeHammingWindow(int windowLength) {
        // Make a Hamming window:
        // w(n) = a0 - (1-a0)*cos( 2*PI*n/(N-1) )
        // a0 = 25/46
        double a0 = 25.0/46.0;
        double[] window = new double[windowLength];
        for(int i = 0; i < windowLength; i++)
            window[i] = a0 - (1-a0) * Math.cos(2*Math.PI*i/(windowLength-1));
        return window;
    }

    private void makePlot() {

        // Plots the signal

        List<Entry> entries;
        LineDataSet dataSet;
        LineData lineData = new LineData();
        Legend myLegend = chart.getLegend();
        List<LegendEntry> myLegendEntries = new ArrayList<>();

        entries = new ArrayList<>();
        int index = 0;
        int colorIndex = 0;
        String oldTag = "None";
        String newTag="";
        while (index < xInterp.size()) {
            newTag = findTag(xInterp.get(index));
            if (oldTag != newTag) {
                Log.i(LogTag + ".Tag","Tag change instant: "+xInterp.get(index) + "  -  New tag: "+newTag);
                dataSet = new LineDataSet(entries, oldTag);
                if  (oldTag == "None") {
                    dataSet.setLabel("");
                    dataSet.setColor(Color.BLACK);
                } else {
                    if (colorIndex>MAX_TAGS-1) {
                        throw new IllegalArgumentException("Number of colors in the plot exceeded limit");
                    }
                    dataSet.setColor(ColorTemplate.COLORFUL_COLORS[colorIndex]);
                    LegendEntry newEntry = new LegendEntry(
                            oldTag, Legend.LegendForm.SQUARE, java.lang.Float.NaN, java.lang.Float.NaN,
                            null, ColorTemplate.COLORFUL_COLORS[colorIndex]);
                    myLegendEntries.add(newEntry);
                    colorIndex++;
                }
                dataSet.setDrawCircles(false);
                lineData.addDataSet(dataSet);
                entries = new ArrayList<>();
                oldTag = newTag;
            }
            entries.add(new Entry(xInterp.get(index), dataHRInterp.get(index)));
            index++;
        }
        dataSet = new LineDataSet(entries, oldTag);
        if  (oldTag == "None") {
            dataSet.setLabel("");
            dataSet.setColor(Color.BLACK);
        } else {
            if (colorIndex>MAX_TAGS-1) {
                throw new IllegalArgumentException("Number of colors in the plot exceeded limit");
            }
            dataSet.setColor(ColorTemplate.COLORFUL_COLORS[colorIndex]);
            LegendEntry newEntry = new LegendEntry(
                    oldTag, Legend.LegendForm.SQUARE, java.lang.Float.NaN, java.lang.Float.NaN,
                    null, ColorTemplate.COLORFUL_COLORS[colorIndex]);
            myLegendEntries.add(newEntry);
        }
        dataSet.setDrawCircles(false);
        lineData.addDataSet(dataSet);

        chart.setData(lineData);
        chart.setPinchZoom(true);
        chart.setHighlightPerTapEnabled(false);
        chart.setHighlightPerDragEnabled(false);

        chart.getDescription().setText("Heart-rate (bps) vs time (sec.)");
        myLegend.setCustom(myLegendEntries);

        chart.invalidate(); // refresh

    }

    private LineChart chart;
    private TextView boxdata;
    private List<Float> dataRRnf, dataHRnf, dataBeatTimesnf, dataBeatTimes, dataRR, dataHR, xInterp, dataHRInterp;
    private List<Float> episodesInits;
    private List<Float> episodesEnds;
    private List<String> episodesType;
    private float freq = 4.0f;  // Interpolation frequency in hz.
    private int numOfTags = 0;
    private static int MAX_TAGS = 5;

    public static File beatsFile;
    public static File tagsFile;
}
