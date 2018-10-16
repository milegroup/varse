package com.devbaltasarq.varse.ui.showresult;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.ui.AppActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Represents the result data set as a graph on the screen.
  * public static File beatsFile y tagsFile deben ser cargadas primero.
  * @author Leandro (removed chart dependency by baltasarq)
  */
public class ResultViewerActivity extends AppActivity {
    private static final String LogTag = ResultViewerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_result_viewer );

        // Back button
        ImageButton btBack = this.findViewById( R.id.btCloseResultViewer );
        btBack.setOnClickListener( (v) -> this.finish() );

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

        //chart = findViewById(R.id.chart);
        boxdata = findViewById(R.id.lblTextData);
        boxdata.setMovementMethod(new ScrollingMovementMethod());

        // Loads data into dataRRnf (unfiltered RR in milliseconds)
        dataRRnf = new ArrayList<>();

        // Loads tags file
        episodesInits = new ArrayList<>();
        episodesEnds = new ArrayList<>();
        episodesType = new ArrayList<>();

        this.loadData( beatsFile );
        this.loadTags( tagsFile );

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
            dataHRInterpX = new ArrayList<>();
            dataHRInterp = new ArrayList<>();
            Interpolate();

            Log.i(LogTag,"length of xinterp: "+ dataHRInterpX.size());
            Log.i(LogTag,"First value: "+ dataHRInterpX.get(0));
            Log.i(LogTag,"Last value: "+ dataHRInterpX.get(dataHRInterpX.size()-1));

            // Plots interpolated HR signal
            plotChart();


            Analyze();
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

    private void loadData(File f)
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
            this.dataRRnf.clear();
        }
    }

    private void loadTags(File f) {
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
                        float init = 3600.0f * Float.parseFloat(hms[0]) + 60.0f*Float.parseFloat(hms[1]) + Float.parseFloat(hms[2]);
                        float end = init + Float.parseFloat(parts[2]);

                        Log.i(LogTag + ".Tags", "Tag: "+parts[1]);
                        Log.i(LogTag + ".Tags", "Init: "+parts[0]+" ("+init+" seconds)");
                        Log.i(LogTag + ".Tags", "End: "+end+" seconds");

                        // Only now create the episode time marks
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

    /** @return the tag corresponding to the given time value. */
    private String findTag(float timeValue)
    {
        String tag = this.getString( R.string.lblDefaultTag );

        for (int i = 0; i < episodesType.size(); i++) {
            if ( ( episodesInits.get( i ) <= timeValue )
              && ( episodesEnds.get( i ) >= timeValue ) )
            {
                tag = episodesType.get( i );
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
        float ulast = 13.0f;
        dataRR = new ArrayList<>(dataRRnf);
        Log.i(LogTag,"I'm going to filter the signal");

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
        dataHRInterpX.add(xmin);
        float newValue = xmin+step;
        while (newValue<=xmax) {
            dataHRInterpX.add(newValue);
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

        for (int xInterpIndex = 0; xInterpIndex< dataHRInterpX.size(); xInterpIndex++ ){
            if (dataHRInterpX.get(xInterpIndex)>=rightBeatPos) {
                leftHRIndex++;
                rightHRIndex++;
                leftBeatPos = dataBeatTimes.get(leftHRIndex);
                rightBeatPos = dataBeatTimes.get(rightHRIndex);
                leftHRVal = dataHR.get(leftHRIndex);
                rightHRVal = dataHR.get(rightHRIndex);
            }

            // Estimate HR value in position
            float HR = (rightHRVal-leftHRVal)*(dataHRInterpX.get(xInterpIndex)-leftBeatPos)/(rightBeatPos-leftBeatPos)+leftHRVal;
            dataHRInterp.add(HR);

        }

    }

    private List<Float> getSegmentHRInterp(float beg, float end) {
        List<Float> segment = new ArrayList<>();
        for (int indexHR=0 ; indexHR<dataHRInterp.size() ; indexHR++) {
            if  ( (dataHRInterpX.get(indexHR) >= beg) && (dataHRInterpX.get(indexHR) <= end) ) {
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

    @SuppressWarnings("deprecation")
    private void Analyze() {
        final StringBuilder text = new StringBuilder( "<h3>Signal data</h3>" );
        final float FILTERED_RATE = 100.0f * (dataRRnf.size() - dataRR.size()) / dataRRnf.size();

        text.append( "<p>&nbsp;&nbsp;<b>Length of original RR signal</b>: " );
        text.append( String.format( Locale.getDefault(), "%d", dataRRnf.size()) );
        text.append( " values</p>" );
        text.append( "<p>&nbsp;&nbsp;<b>Length of filtered RR signal</b>: " );
        text.append( String.format( Locale.getDefault(), "%d", dataRR.size()) );
        text.append( " values</p>" );

        text.append( "<p>&nbsp;&nbsp;<b>Beat rejection rate</b>: " );
        text.append( String.format( Locale.getDefault(), "%.2f", FILTERED_RATE) );
        text.append( "%</p>" );
        text.append( "<p>&nbsp;&nbsp;<b>Interpolation frequency</b>: " );
        text.append( String.format( Locale.getDefault(), "%.2f", freq) );
        text.append( " Hz</p>" );
        text.append( "<p>&nbsp;&nbsp;<b>Number of interpolated samples</b>: " );
        text.append( String.format( Locale.getDefault(), "%d", dataHRInterp.size()) );
        text.append( "</p>" );

        // ------------------------

        text.append( "<br/><h3>HRV time-domain results</h3>" );
        text.append( "<p>&nbsp;&nbsp;<b>Mean RR (AVNN)</b>: " );
        text.append( String.format( Locale.getDefault(), "%.2f", calculateMean(dataRR)) );
        text.append( " ms</p>" );
        text.append( "<p>&nbsp;&nbsp;<b>STD RR (SDNN)</b>: " );
        text.append( String.format( Locale.getDefault(), "%.2f", calculateSTD(dataRR)) );
        text.append( " ms</p>" );
        text.append( "<p>&nbsp;&nbsp;<b>pNN50</b>: " );
        text.append( String.format( Locale.getDefault(), "%.2f", calculatePNN50(dataRR)) );
        text.append( "%</p>" );
        text.append( "<p>&nbsp;&nbsp;<b>rMSSD</b>: " );
        text.append( String.format( Locale.getDefault(), "%.2f", calculateRMSSD(dataRR)) );
        text.append( " ms</p>" );
        text.append( "<p>&nbsp;&nbsp;<b>normHRV</b>: " );
        text.append( String.format( Locale.getDefault(), "%.2f", calculateNormHRV(dataRR)) );
        text.append( "</p>" );

        // ------------------------
        final List<Float> powerbands = calculateSpectrum(dataHRInterpX.get(0), dataHRInterpX.get(dataHRInterpX.size()-1));

        text.append( "<br/><h3>HRV frequency-domain results</h3>" );
        text.append( "<p>&nbsp;&nbsp;<b>Total power</b>: " );
        text.append( String.format( Locale.getDefault(), "%.2f", powerbands.get(0)) );
        text.append( " ms&sup2;</p>" );

        if (powerbands.get(1)>0.0) {
            text.append( "<p>&nbsp;&nbsp;<b>LF power</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", powerbands.get(1)) );
            text.append( " ms&sup2;</p>" );
        } else {
            text.append( "<p>&nbsp;&nbsp;<b>LF power</b>: --</p>" );
        }

        if (powerbands.get(2)>0.0) {
            text.append( "<p>&nbsp;&nbsp;<b>HF power</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", powerbands.get(2)) );
            text.append( " ms&sup2;</p>" );
        } else {
            text.append( "<p>&nbsp;&nbsp;<b>HF power</b>: --</p>" );
        }

        if (powerbands.get(1)>0.0) {
            text.append( "<p>&nbsp;&nbsp;<b>LF/HF ratio</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", powerbands.get(3)) );
            text.append( "</p>" );
        } else {
            text.append( "<p>&nbsp;&nbsp;<b>LF/HF ratio</b>: --</p>" );
        }

        // ------------------------

        for (int tagIndex = 0; tagIndex < numOfTags; tagIndex++) {
            float length = episodesEnds.get(tagIndex)-episodesInits.get(tagIndex);
            List<Float> tagSegment = getSegmentRR(tagIndex);

            text.append( "<br/><h3> Tag: " );
            text.append( episodesType.get(tagIndex) );
            text.append( "</h3>" );

            text.append( "<p>&nbsp;&nbsp;<b>Length</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", length) );
            text.append( " s</p>" );

            text.append( "<p>&nbsp;&nbsp;<b>Mean RR (AVNN)</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", calculateMean(tagSegment)) );
            text.append( " ms</p>" );

            text.append( "<p>&nbsp;&nbsp;<b>STD RR (SDNN)</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", calculateSTD(tagSegment)) );
            text.append( " ms</p>" );
            text.append( "<p>&nbsp;&nbsp;<b>pNN50</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", calculatePNN50(tagSegment)) );
            text.append( "%</p>" );
            text.append( "<p>&nbsp;&nbsp;<b>rMSSD</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", calculateRMSSD(tagSegment)) );
            text.append( " ms</p>" );
            text.append( "<p>&nbsp;&nbsp;<b>normHRV</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", calculateNormHRV(tagSegment)) );
            text.append( "</p>" );


            List<Float> tagPowerbands = calculateSpectrum(episodesInits.get(tagIndex), episodesEnds.get(tagIndex));
            text.append( "<p>&nbsp;&nbsp;<b>Total spectral power</b>: " );
            text.append( String.format( Locale.getDefault(), "%.2f", tagPowerbands.get(0)) );
            text.append( " ms&sup2;</p>" );

            if (tagPowerbands.get(1)>0.0) {
                text.append( "<p>&nbsp;&nbsp;<b>LF power</b>: " );
                text.append( String.format( Locale.getDefault(), "%.2f", tagPowerbands.get(1)) );
                text.append( " ms&sup2;</p>" );
            } else {
                text.append( "<p>&nbsp;&nbsp;<b>LF power</b>: --</p>" );
            }

            if (tagPowerbands.get(2)>0.0) {
                text.append( "<p>&nbsp;&nbsp;<b>HF power</b>: " );
                text.append( String.format( Locale.getDefault(), "%.2f", tagPowerbands.get(2)) );
                text.append( " ms&sup2;</p>" );
            } else {
                text.append( "<p>&nbsp;&nbsp;<b>HF power</b>: --</p>" );
            }

            if (tagPowerbands.get(1)>0.0) {
                text.append( "<p>&nbsp;&nbsp;<b>LF/HF ratio</b>: " );
                text.append( String.format( Locale.getDefault(), "%.2f", tagPowerbands.get(3)) );
                text.append( "</p>" );
            } else {
                text.append( "<p>&nbsp;&nbsp;<b>LF/HF ratio</b>: --</p>" );
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            boxdata.setText(Html.fromHtml(text.toString(), Html.FROM_HTML_MODE_COMPACT));
        } else {
            boxdata.setText(Html.fromHtml(text.toString()));
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

    private List<Float> calculateSpectrum(Float begSegment, Float endSegment) {
        Log.i(LogTag + ".Spec","Calculating spectrum");
        Log.i(LogTag + ".Spec","Minimum time: " + begSegment + " seconds");
        Log.i(LogTag + ".Spec","Maximum time: " + endSegment + " seconds");
        float analysisWindowLength = ( endSegment-begSegment ) / 3.0f;
        Log.i(LogTag + ".Spec","Analysis window length: "+ analysisWindowLength +" seconds");

        // Five windows, length 1/3 of signal, overlap 50%

        float beg[] = new float[5];
        float end[] = new float[5];

        beg[0] = begSegment;
        end[0] = beg[0] + analysisWindowLength;

        for (int index=1 ; index < 5; index++ ) {
            beg[index] = beg[index-1] + analysisWindowLength / 2.0f;
            end[index] = beg[index] + analysisWindowLength;
        }

        for (int index=0; index < 5; index++) {
            Log.i(LogTag + ".Spec","Window number "+ (index+1) +": ("+ beg[index] + "," + end[index] +") seconds");
        }

        int maxSegmentLength = 0;
        for (int index=0; index<5; index++) {
            List<Float> segmentTMP;
            segmentTMP = getSegmentHRInterp(beg[index],end[index]);
            if ( segmentTMP.size() > maxSegmentLength )
                maxSegmentLength = segmentTMP.size();
        }

        int paddedLength = (int) Math.pow(2,(int) Math.ceil(Math.log((double) maxSegmentLength) / Math.log(2.0)));

        Log.i(LogTag + ".Spec","Max segment length: "+maxSegmentLength);
        Log.i(LogTag + ".Spec","Padded length: "+paddedLength);


        List<Float> SpectrumAvg = new ArrayList<>();
        int SpectrumLength = paddedLength/2;


        for (int windowIndex=0 ; windowIndex<5 ; windowIndex++) {
            List<Float> RRSegment = getSegmentHRInterp(beg[windowIndex],end[windowIndex]);
            for  (int index=0 ; index < RRSegment.size() ; index++) {
                RRSegment.set(index,1000.0f/(RRSegment.get(index)/60.f));
            }
            Log.i(LogTag + ".Spec", "Segment "+(windowIndex+1)+" - number of samples: "+RRSegment.size());
            double avg = 0.0;
            for  (int index=0 ; index < RRSegment.size() ; index++) {
                avg += RRSegment.get(index);
            }
            avg = avg / RRSegment.size();
            for  (int index=0 ; index < RRSegment.size() ; index++) {
                RRSegment.set( index , (float) (RRSegment.get(index)-avg) );
            }

            double[] hamWindow = makeHammingWindow(RRSegment.size());
            for (int index=0 ; index < RRSegment.size() ; index++) {
                RRSegment.set(index, (float) (RRSegment.get(index)*hamWindow[index]));
            }

            // writeFile("timeSignal.txt", RRSegment);

            double[] RRSegmentPaddedX = padSegmentHRInterp(RRSegment, paddedLength);
            //Log.i(LogTag + ".Spec", "Length of padded array: "+RRSegmentPaddedX.length);

            /*
            List<Double> RRSPtmp = new ArrayList<>();
            for (double rrPaddedX: RRSegmentPaddedX) {
                RRSPtmp.add( rrPaddedX );
            }
            writeFile("timeSignalPadded.txt", RRSPtmp);
            */
            double[] RRSegmentPaddedY = new double[RRSegmentPaddedX.length];

            fft(RRSegmentPaddedX,RRSegmentPaddedY, RRSegmentPaddedX.length);
            //Log.i(LogTag + ".Spec","Length of fft: "+RRSegmentPaddedX.length);

            List<Float> Spectrum = new ArrayList<>();
            for (int index=0 ; index<SpectrumLength ; index++) { // Only positive half of the spectrum
                Spectrum.add((float) (Math.pow(RRSegmentPaddedX[index],2)+Math.pow(RRSegmentPaddedY[index],2)));
            }
            Log.i(LogTag + ".Spec","Length of spectrum: "+Spectrum.size());

            if (windowIndex==0) {
                for (int index=0 ; index<SpectrumLength ; index++) {
                    SpectrumAvg.add(Spectrum.get(index));
                }
            } else {
                for (int index=0 ; index<SpectrumLength ; index++) {
                    float newValue = SpectrumAvg.get(index)+Spectrum.get(index);
                    SpectrumAvg.set(index,newValue);
                }
            }

        }  // for windowIndex

        for (int index=0 ; index<SpectrumLength ; index++) {
            SpectrumAvg.set(index,SpectrumAvg.get(index)/5.0f);
        }

        List<Float> SpectrumAxis = new ArrayList<>();
        for (int index=0 ; index<SpectrumLength ; index++) { // Only positive half of the spectrum
            SpectrumAxis.add(index*(freq/2)/(SpectrumLength-1));
        }

        // writeFile("Spectrum.txt", SpectrumAvg);

        Log.i(LogTag + ".Spec","Length of spectrum axis: "+SpectrumAxis.size());

        if ( SpectrumAxis.size() > 0 ) {
            Log.i(LogTag + ".Spec","First sample of spectrum axis: "+SpectrumAxis.get(0));
            Log.i(LogTag + ".Spec","Last sample of spectrum axis: "+SpectrumAxis.get(SpectrumLength-1));
        }

        List<Float> results = new ArrayList<>();

        float totalPower = powerInBand(SpectrumAvg, SpectrumAxis, totalPowerBeg, totalPowerEnd);

        results.add(totalPower);

        Log.i(LogTag + ".Spec", "Total power: "+totalPower);



        float LFPower;
        if ((endSegment-begSegment) > 40.0) {
            // Minimum freq. in LF band is 0.05 Hz. Two cycles are required to estimate power
            LFPower = powerInBand(SpectrumAvg, SpectrumAxis, LFPowerBeg, LFPowerEnd);
        } else {
            LFPower = -1.0f;
        }
        results.add(LFPower);
        Log.i(LogTag + ".Spec", "LF power: "+LFPower);

        float HFPower;
        if ((endSegment-begSegment) > 13.33) {
            HFPower = powerInBand(SpectrumAvg, SpectrumAxis, HFPowerBeg, HFPowerEnd);
        } else {
            HFPower = -1.0f;
        }
        results.add(HFPower);
        Log.i(LogTag + ".Spec", "HF power: "+HFPower);
        Log.i(LogTag + ".Spec", "LF/HF ratio: "+LFPower/HFPower);
        results.add(LFPower/HFPower);

        return results;
    }

    private void fft(double[] x, double[] y, int n)
    {
        int i,j,k,n2,a;
        int n1;
        double c,s,t1,t2;

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


    private double[] makeHammingWindow(int windowLength) {
        // Make a Hamming window:
        // w(n) = a0 - (1-a0)*cos( 2*PI*n/(N-1) )
        // a0 = 25/46
        double a0 = 25.0/46.0;
        double[] window = new double[windowLength];
        for(int i = 0; i < windowLength; i++)
            window[i] = a0 - (1-a0) * Math.cos(2*Math.PI*i/(windowLength-1));
        return window;
    }

    private float powerInBand(List<Float> spectrum, List<Float> spectrumAxis, float begFreq, float endFreq) {
        float pp = 0.0f;
        for (int index=0 ; index < spectrum.size() ; index++) {
            if ( (spectrumAxis.get(index)>=begFreq) && (spectrumAxis.get(index)<=endFreq) ) {
                pp = pp + spectrum.get(index);
            }
        }
        pp = pp * hammingFactor;
        pp = pp / (float)(2.0f*Math.pow(spectrum.size(),2.0f));
        return pp;
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

    /** Plots the chart in a drawable and shows it. */
    private void plotChart()
    {
        final double DENSITY = this.getResources().getDisplayMetrics().scaledDensity;
        final ArrayList<LineChart.SeriesInfo> SERIES = new ArrayList<>();
        final ArrayList<LineChart.Point> POINTS = new ArrayList<>();
        final ImageView CHART_VIEWER = findViewById( R.id.ivChartViewer );

        if ( dataHRInterpX.size() > 0 ) {
            String tag = this.findTag( dataHRInterpX.get( 0 ) );
            int color = this.calculateColor( tag, 0 );
            int index = 0;
            int colorIndex = 0;

            SERIES.add( new LineChart.SeriesInfo( tag, color ) );

            for(float time: this.dataHRInterpX ) {
                final double BPM = this.dataHRInterp.get( index );
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
        final LineChart chart = new LineChart( DENSITY, POINTS, SERIES );
        chart.setLegendY( "Heart-rate (bpm)" );
        chart.setLegendX( "Time (sec.)" );
        chart.setShowLabels( false );
        CHART_VIEWER.setBackground( chart );
    }

    TextView boxdata;
    List<Float> dataRRnf;
    List<Float> dataHRnf;
    List<Float> dataBeatTimesnf;
    List<Float> dataBeatTimes;
    List<Float> dataRR;
    List<Float> dataHR;
    List<Float> dataHRInterpX;
    List<Float> dataHRInterp;
    List<Float> episodesInits;
    List<Float> episodesEnds;
    List<String> episodesType;
    static float freq = 4.0f;                   // Interpolation frequency in hz.
    int numOfTags = 0;
    static int MAX_TAGS = 5;

    static float hammingFactor = 1.586f;

    static float totalPowerBeg = 0.0f;
    static float totalPowerEnd = 4.0f/2.0f;     // Beginning and end of total power band

    static float LFPowerBeg = 0.05f;
    static float LFPowerEnd = 0.15f;            // Beginning and end of LF band

    static float HFPowerBeg = 0.15f;
    static float HFPowerEnd = 0.4f;             // Beginning and end of HF band

    public static File beatsFile;
    public static File tagsFile;
}
