// Varse (c) 2019/20 Baltasar MIT License <jbgarcia@uvigo.es>


package com.devbaltasarq.varse.core;


import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class ResultAnalyzer {
    private static final String LOG_TAG = ResultAnalyzer.class.getSimpleName();

    public ResultAnalyzer(final Result RESULT, final int MAX_TAGS)
    {
        this.numOfTags = 0;
        this.result = RESULT;

        this.dataRRnf = new ArrayList<>( this.result.size() );
        this.episodesInits = new ArrayList<>( MAX_TAGS );
        this.episodesEnds = new ArrayList<>( MAX_TAGS );
        this.episodesType = new ArrayList<>( MAX_TAGS );
    }

    public void analyze()
    {
        // Loads data into dataRRnf and episodes (unfiltered RR in milliseconds)
        this.loadData();

        if ( !this.dataRRnf.isEmpty() ) {
            // Generates dataHRnf (unfiltered sequence of BPS values)
            ArrayList<Float> dataHRnf = new ArrayList<>( dataRRnf.size() );
            for (int i = 0; i < dataRRnf.size(); i++) {
                dataHRnf.add(60.0f/(dataRRnf.get(i)/1000.0f));
            }

            // Calculates dataBeatTimesnf (unfiltered beat positions in seconds) from dataRRnf
            ArrayList<Float> dataBeatTimesnf = new ArrayList<>( dataRRnf.size() );
            dataBeatTimesnf.add(dataRRnf.get(0)/1000.0f);
            for (int i = 1; i < dataRRnf.size(); i++) {
                dataBeatTimesnf.add( dataBeatTimesnf.get(i-1) + ( dataRRnf.get(i)/1000.0f ) );
            }

            // Filters beat times creating a sequence of RR intervals
            dataBeatTimes = new ArrayList<>(dataBeatTimesnf);
            dataHR = new ArrayList<>(dataHRnf);
            dataRR = new ArrayList<>(dataRRnf);
            FilterData();

            Log.i(LOG_TAG,"Filtered sequence: " + dataBeatTimes.size() +" values");
            Log.i(LOG_TAG,"Last beat position: " + dataBeatTimes.get( dataBeatTimes.size() - 1 ) + " seconds");

            // Creates a series of HR values linearly interpolated
            dataHRInterpX = new ArrayList<>( dataRRnf.size() );
            dataHRInterp = new ArrayList<>( dataRRnf.size() );
            Interpolate();

            Log.i(LOG_TAG,"length of xinterp: "+ dataHRInterpX.size());
            Log.i(LOG_TAG,"First value: "+ dataHRInterpX.get(0));
            Log.i(LOG_TAG,"Last value: "+ dataHRInterpX.get(dataHRInterpX.size()-1));

            // Calculate stress level
            this.valueRMS = this.calculateRMSSD( this.dataRR );
            this.valueSTD = this.calculateSTD( this.dataRR );
            this.valueMeanBPM = this.calculateMean( this.dataHR );
            this.valuePNN50 = this.calculatePNN50( this.dataRR );
            this.valueMADRR = this.calculateMADRR( this.dataRR );       // Median
            this.valueApEn = this.calculateApEn( this.dataRR, 2, 0.2f ); // Entropy
        }
    }

    private void loadData()
    {
        final List<Result.Event> EVENTS = this.result.buildEventsList();
        Result.ActivityChangeEvent lastTagEvent = null;

        // Init data holders
        this.numOfTags = 0;

        // Store all data
        for(Result.Event evt: EVENTS) {
            if ( evt instanceof final Result.BeatEvent EVT) {
                // Store a new beat
                this.dataRRnf.add( (float) ( EVT.getTimeOfNewHeartBeat() ));
            } else {
                // Store the tags info
                final Result.ActivityChangeEvent TAG_EVENT = (Result.ActivityChangeEvent) evt;
                final float TIME_IN_SECS = (float) TAG_EVENT.getMillis() / 1000;

                this.episodesEnds.add( (float) result.getDurationInMillis() / 1000 );

                if ( lastTagEvent != null ) {
                    this.episodesEnds.set( this.episodesEnds.size() - 2, TIME_IN_SECS );
                }

                this.episodesInits.add( TIME_IN_SECS );
                this.episodesType.add( TAG_EVENT.getTag().getHumanReadable() );
                lastTagEvent = TAG_EVENT;
                ++this.numOfTags;
            }
        }

        Log.i(LOG_TAG,"Size of vector: " + this.dataRRnf.size() );
    }

    private List<Float> getSegmentRR(int tagIndex)
    {
        final float EPISODE_INIT = episodesInits.get( tagIndex );
        final float EPISODE_END = episodesEnds.get( tagIndex );
        final int APPROX_LENGTH = (int) ( EPISODE_END - EPISODE_INIT ) * 2;
        final List<Float> TORET = new ArrayList<>( APPROX_LENGTH );

        Log.i(LOG_TAG,"Getting segment corresponding to tag: " + episodesType.get( tagIndex ) );

        for (int indexRR = 0; indexRR < dataRR.size(); indexRR++) {
            final boolean IS_OVER_LOWER_LIMIT = dataBeatTimes.get(indexRR) >= EPISODE_INIT;
            final boolean IS_UNDER_UPPER_LIMIT = dataBeatTimes.get(indexRR) <= EPISODE_END;

            if ( !IS_OVER_LOWER_LIMIT ) {
                continue;
            }
            else
            if ( !IS_UNDER_UPPER_LIMIT ) {
                break;
            }

            TORET.add( dataRR.get( indexRR ) );
        }

        Log.i(LOG_TAG,"Num of values: "+ TORET.size() );

        return TORET;
    }

    private void FilterData()
    {
        final int WIN_LENGTH = 50;
        final float MIN_BPM = 24.0f;
        final float MAX_BPM = 198.0f;
        final float U_LAST = 13.0f;
        final float U_MEAN = 1.5f * U_LAST;

        Log.i(LOG_TAG,"I'm going to filter the signal");

        int index = 1;

        this.filteredData = 0;

        while ( index < ( dataHR.size() - 1 ) ) {
            List<Float> v = dataHR.subList( Math.max( index-WIN_LENGTH, 0 ),index );

            float MEAN_LAST_BEATS = 0.0f;  // M = mean(v)
            for (int i = 0 ; i < v.size(); i++) {
                MEAN_LAST_BEATS += v.get( i );
            }
            MEAN_LAST_BEATS = MEAN_LAST_BEATS / v.size();

            final float CURRENT_BEAT = this.dataHR.get( index );
            final float PREVIOUS_BEAT = this.dataHR.get( index - 1 );
            final float NEXT_BEAT = this.dataHR.get( index + 1 );
            final float RELATION_PREVIOUS_BEAT = 100
                    * Math.abs( ( CURRENT_BEAT - PREVIOUS_BEAT ) / PREVIOUS_BEAT );
            final float RELATION_NEXT_BEAT = 100
                    * Math.abs( ( CURRENT_BEAT - NEXT_BEAT ) / NEXT_BEAT );

            final float RELATION_MEAN_BEAT = 100
                    * Math.abs( ( CURRENT_BEAT - MEAN_LAST_BEATS ) / MEAN_LAST_BEATS );

            if ( ( RELATION_PREVIOUS_BEAT < U_LAST
                    || RELATION_NEXT_BEAT < U_LAST
                    || RELATION_MEAN_BEAT < U_MEAN )
                    && CURRENT_BEAT > MIN_BPM
                    && CURRENT_BEAT < MAX_BPM )
            {
                index += 1;
            } else {
                Log.i(LOG_TAG,"Removing beat index: " + index );

                index += 1;
                ++this.filteredData;
                this.dataHR.set( index, MEAN_LAST_BEATS );
                this.dataRR.set( index, 60.0f / MEAN_LAST_BEATS );
            }
        }

        return;
    }

    private void Interpolate()
    {
        float xmin = dataBeatTimes.get(0);
        float xmax = dataBeatTimes.get(dataBeatTimes.size()-1);
        float step = 1.0f / freq;

        if ( dataBeatTimes.size() > 2 ) {
            int leftHRIndex, rightHRIndex;
            float leftBeatPos, rightBeatPos, leftHRVal, rightHRVal;
            leftHRIndex = 0;
            rightHRIndex = 1;
            leftBeatPos = dataBeatTimes.get( leftHRIndex );
            rightBeatPos = dataBeatTimes.get( rightHRIndex );
            leftHRVal = dataHR.get(leftHRIndex);
            rightHRVal = dataHR.get(rightHRIndex);

            // Calculates positions in x axis
            dataHRInterpX.add(xmin);
            float newValue = xmin+step;
            while (newValue<=xmax) {
                dataHRInterpX.add(newValue);
                newValue += step;
            }

            for (int xInterpIndex = 0; xInterpIndex< dataHRInterpX.size() - 1; xInterpIndex++ )
            {
                if (dataHRInterpX.get(xInterpIndex) >= rightBeatPos) {
                    leftHRIndex++;
                    rightHRIndex++;
                    leftBeatPos = dataBeatTimes.get( leftHRIndex );
                    rightBeatPos = dataBeatTimes.get( rightHRIndex );
                    leftHRVal = dataHR.get( leftHRIndex );
                    rightHRVal = dataHR.get( rightHRIndex );
                }

                // Estimate HR value in position
                float HR = (rightHRVal-leftHRVal)*(dataHRInterpX.get(xInterpIndex)-leftBeatPos)/(rightBeatPos-leftBeatPos)+leftHRVal;
                dataHRInterp.add(HR);
            }
        } else {
            float xAxis = xmin;

            for(float dataBeatTime: this.dataBeatTimes) {
                this.dataHRInterp.add( dataBeatTime );
                this.dataHRInterpX.add( xAxis );

                xAxis += step;
            }
        }

        return;
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

    private double[] padSegmentHRInterp(List<Float> hrSegment, int newLength)
    {
        double[] segmentPadded = new double[ newLength ];

        for (int index = 0 ; index < hrSegment.size() ; index++) {
            segmentPadded[ index ] =  (double) ( hrSegment.get( index ) );
        }

        return segmentPadded;
    }

    public String buildReport()
    {
        final StringBuilder TEXT = new StringBuilder( "<h3>Signal data</h3>" );
        final float FILTERED_RATE = 100.0f * (dataRRnf.size() - dataRR.size()) / dataRRnf.size();

        TEXT.append( "<p>&nbsp;&nbsp;<b>Length of original RR signal</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%d", dataRRnf.size()) );
        TEXT.append( " values</p>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>Length of filtered RR signal</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%d", dataRRnf.size() - this.filteredData) );
        TEXT.append( " values</p>" );

        TEXT.append( "<p>&nbsp;&nbsp;<b>Beat rejection rate</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", FILTERED_RATE) );
        TEXT.append( "%</p>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>Interpolation frequency</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", freq) );
        TEXT.append( " Hz</p>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>Number of interpolated samples</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%d", dataHRInterp.size()) );
        TEXT.append( "</p>" );

        // ------------------------

        TEXT.append( "<br/><h3>HRV time-domain results</h3>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>Mean RR (AVNN)</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", this.valueMeanBPM ) );
        TEXT.append( " ms</p>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>STD RR (SDNN)</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", this.valueSTD ) );
        TEXT.append( " ms</p>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>pNN50</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", this.valuePNN50 ) );
        TEXT.append( "%</p>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>rMSSD</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", this.valueRMS ) );
        TEXT.append( " ms</p>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>normHRV</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", calculateNormHRV(dataRR)) );
        TEXT.append( "</p>" );

        TEXT.append( "<p>&nbsp;&nbsp;<b>MadRR</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", this.valueMADRR ) );
        TEXT.append( "ms.</p>" );

        // ------------------------

        TEXT.append( "<br/><h3>Non-linear indexes</h3>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>ApEn</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", this.valueApEn ) );
        TEXT.append( "ms.</p>" );

        // ------------------------

        final List<Float> powerbands = calculateSpectrum(dataHRInterpX.get(0), dataHRInterpX.get(dataHRInterpX.size()-1));

        TEXT.append( "<br/><h3>HRV frequency-domain results</h3>" );
        TEXT.append( "<p>&nbsp;&nbsp;<b>Total power</b>: " );
        TEXT.append( String.format( Locale.getDefault(), "%.2f", powerbands.get(0)) );
        TEXT.append( " ms&sup2;</p>" );

        if (powerbands.get(1)>0.0) {
            TEXT.append( "<p>&nbsp;&nbsp;<b>LF power</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", powerbands.get(1)) );
            TEXT.append( " ms&sup2;</p>" );
        } else {
            TEXT.append( "<p>&nbsp;&nbsp;<b>LF power</b>: --</p>" );
        }

        if (powerbands.get(2)>0.0) {
            TEXT.append( "<p>&nbsp;&nbsp;<b>HF power</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", powerbands.get(2)) );
            TEXT.append( " ms&sup2;</p>" );
        } else {
            TEXT.append( "<p>&nbsp;&nbsp;<b>HF power</b>: --</p>" );
        }

        if (powerbands.get(1)>0.0) {
            TEXT.append( "<p>&nbsp;&nbsp;<b>LF/HF ratio</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", powerbands.get(3)) );
            TEXT.append( "</p>" );
        } else {
            TEXT.append( "<p>&nbsp;&nbsp;<b>LF/HF ratio</b>: --</p>" );
        }

        // ------------------------

        for (int tagIndex = 0; tagIndex < numOfTags; tagIndex++) {
            float length = episodesEnds.get(tagIndex) - episodesInits.get(tagIndex);
            List<Float> tagSegment = getSegmentRR( tagIndex );

            TEXT.append( "<br/><h3> Tag: " );
            TEXT.append( episodesType.get(tagIndex) );
            TEXT.append( "</h3>" );

            TEXT.append( "<p>&nbsp;&nbsp;<b>Length</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", length) );
            TEXT.append( " s</p>" );

            TEXT.append( "<p>&nbsp;&nbsp;<b>Mean RR (AVNN)</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", calculateMean(tagSegment)) );
            TEXT.append( " ms</p>" );

            TEXT.append( "<p>&nbsp;&nbsp;<b>STD RR (SDNN)</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", calculateSTD(tagSegment)) );
            TEXT.append( " ms</p>" );
            TEXT.append( "<p>&nbsp;&nbsp;<b>pNN50</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", calculatePNN50(tagSegment)) );
            TEXT.append( "%</p>" );
            TEXT.append( "<p>&nbsp;&nbsp;<b>rMSSD</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", calculateRMSSD(tagSegment)) );
            TEXT.append( " ms</p>" );
            TEXT.append( "<p>&nbsp;&nbsp;<b>normHRV</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", calculateNormHRV( tagSegment )) );
            TEXT.append( "</p>" );

            TEXT.append( "<p>&nbsp;&nbsp;<b>MadRR</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", calculateMADRR( tagSegment ) ) );
            TEXT.append( "ms.</p>" );

            TEXT.append( "<p>&nbsp;&nbsp;<b>ApEn</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", calculateApEn( tagSegment, 2, 0.2f ) ) );
            TEXT.append( "ms.</p>" );

            List<Float> tagPowerbands = calculateSpectrum( episodesInits.get( tagIndex ),
                                                            episodesEnds.get(tagIndex));
            TEXT.append( "<p>&nbsp;&nbsp;<b>Total spectral power</b>: " );
            TEXT.append( String.format( Locale.getDefault(), "%.2f", tagPowerbands.get(0)) );
            TEXT.append( " ms&sup2;</p>" );

            if (tagPowerbands.get(1)>0.0) {
                TEXT.append( "<p>&nbsp;&nbsp;<b>LF power</b>: " );
                TEXT.append( String.format( Locale.getDefault(), "%.2f", tagPowerbands.get(1)) );
                TEXT.append( " ms&sup2;</p>" );
            } else {
                TEXT.append( "<p>&nbsp;&nbsp;<b>LF power</b>: --</p>" );
            }

            if (tagPowerbands.get(2)>0.0) {
                TEXT.append( "<p>&nbsp;&nbsp;<b>HF power</b>: " );
                TEXT.append( String.format( Locale.getDefault(), "%.2f", tagPowerbands.get(2)) );
                TEXT.append( " ms&sup2;</p>" );
            } else {
                TEXT.append( "<p>&nbsp;&nbsp;<b>HF power</b>: --</p>" );
            }

            if (tagPowerbands.get(1)>0.0) {
                TEXT.append( "<p>&nbsp;&nbsp;<b>LF/HF ratio</b>: " );
                TEXT.append( String.format( Locale.getDefault(), "%.2f", tagPowerbands.get(3)) );
                TEXT.append( "</p>" );
            } else {
                TEXT.append( "<p>&nbsp;&nbsp;<b>LF/HF ratio</b>: --</p>" );
            }
        }

        return TEXT.toString();
    }

    private float calculateMean(final List<Float> SIGNAL) {
        float toret = 0f;

        if ( !SIGNAL.isEmpty() ) {
            for(float signalPart: SIGNAL) {
                toret += signalPart;
            }
        }

        return toret / SIGNAL.size();
    }

    private float calculateSTD(final List<Float> SIGNAL)
    {
        final int SIGNAL_LEN = SIGNAL.size();
        float toret = 0f;

        if ( SIGNAL_LEN > 0 ) {
            final double SIGNAL_MEAN = this.calculateMean( SIGNAL );
            double std = 0f;

            for(float signalPart: SIGNAL) {
                std += Math.pow( signalPart - SIGNAL_MEAN, 2 );
            }

            toret = (float) Math.sqrt( std / ( SIGNAL.size() - 1 ) );
        }

        return toret;
    }

    private float calculateRMSSD(List<Float> signal) {
        double rrdifs2 = 0.0f;

        for (int i = 1 ; i < signal.size() ; i++) {
            rrdifs2 += Math.pow( ( signal.get(i) - signal.get( i - 1 ) ), 2 );
        }

        return (float) Math.sqrt( rrdifs2 / ( signal.size() - 1 ) );
    }

    private float calculateNormHRV(List<Float> signal)
    {
        float lnrMSSD = (float) Math.log( this.calculateRMSSD( signal ) );
        return lnrMSSD * 100.0f / 6.5f;
    }

    private float calculatePNN50(final List<Float> SIGNAL)
    {
        final int SIGNAL_LEN = SIGNAL.size();
        int numIntervals = 0;
        int numBigIntervals = 0;
        float toret = 0f;

        if ( SIGNAL_LEN > 0 ) {
            for (int i=1 ; i < SIGNAL_LEN; i++) {
                numIntervals++;

                if (Math.abs(SIGNAL.get(i)-SIGNAL.get(i-1)) > 50.0) {
                    numBigIntervals++;
                }
            }

            toret = 100f * ( (float) numBigIntervals / numIntervals );
        }

        return toret;
    }

    private List<Float> calculateSpectrum(Float begSegment, Float endSegment)
    {
        Log.i(LOG_TAG + ".Spec","Calculating spectrum");
        Log.i(LOG_TAG + ".Spec","Minimum time: " + begSegment + " seconds");
        Log.i(LOG_TAG + ".Spec","Maximum time: " + endSegment + " seconds");
        float analysisWindowLength = ( endSegment-begSegment ) / 3.0f;
        Log.i(LOG_TAG + ".Spec","Analysis window length: "+ analysisWindowLength +" seconds");

        // Five windows, length 1/3 of signal, overlap 50%
        float[] beg = new float[5];
        float[] end = new float[5];

        beg[0] = begSegment;
        end[0] = beg[0] + analysisWindowLength;

        for (int index=1 ; index < 5; index++ ) {
            beg[index] = beg[index-1] + analysisWindowLength / 2.0f;
            end[index] = beg[index] + analysisWindowLength;
        }

        for (int index=0; index < 5; index++) {
            Log.i(LOG_TAG + ".Spec","Window number "+ (index+1) +": ("+ beg[index] + "," + end[index] +") seconds");
        }

        int maxSegmentLength = 0;
        for (int index=0; index<5; index++) {
            List<Float> segmentTMP;
            segmentTMP = getSegmentHRInterp(beg[index],end[index]);
            if ( segmentTMP.size() > maxSegmentLength )
                maxSegmentLength = segmentTMP.size();
        }

        int paddedLength = (int) Math.pow( 2,
                                           (int) Math.ceil( Math.log( maxSegmentLength )
                                                   / Math.log( 2.0 ) ) );

        Log.i(LOG_TAG + ".Spec","Max segment length: " + maxSegmentLength);
        Log.i(LOG_TAG + ".Spec","Padded length: " + paddedLength);

        List<Float> SpectrumAvg = new ArrayList<>();
        int SpectrumLength = paddedLength/2;


        for (int windowIndex=0 ; windowIndex<5 ; windowIndex++) {
            List<Float> RRSegment = this.getSegmentHRInterp( beg[ windowIndex ], end[ windowIndex ] );

            RRSegment.replaceAll(segPart -> 1000.0f / ( segPart / 60.f ) );

            Log.i(LOG_TAG + ".Spec", "Segment "+(windowIndex+1)+" - number of samples: "+RRSegment.size());
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

            double[] RRSegmentPaddedX = padSegmentHRInterp(RRSegment, paddedLength);
            double[] RRSegmentPaddedY = new double[RRSegmentPaddedX.length];

            fft(RRSegmentPaddedX,RRSegmentPaddedY, RRSegmentPaddedX.length);

            List<Float> Spectrum = new ArrayList<>();
            for (int index=0 ; index<SpectrumLength ; index++) { // Only positive half of the spectrum
                Spectrum.add((float) (Math.pow(RRSegmentPaddedX[index],2)+Math.pow(RRSegmentPaddedY[index],2)));
            }
            Log.i(LOG_TAG + ".Spec","Length of spectrum: "+Spectrum.size());

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

        Log.i(LOG_TAG + ".Spec","Length of spectrum axis: "+SpectrumAxis.size());

        if ( !SpectrumAxis.isEmpty() ) {
            Log.i(LOG_TAG + ".Spec","First sample of spectrum axis: "+SpectrumAxis.get(0));
            Log.i(LOG_TAG + ".Spec","Last sample of spectrum axis: "+SpectrumAxis.get(SpectrumLength-1));
        }

        List<Float> results = new ArrayList<>();

        float totalPower = powerInBand(SpectrumAvg, SpectrumAxis, totalPowerBeg, totalPowerEnd);

        results.add(totalPower);

        Log.i(LOG_TAG + ".Spec", "Total power: "+totalPower);



        float LFPower;
        if ((endSegment-begSegment) > 40.0) {
            // Minimum freq. in LF band is 0.05 Hz. Two cycles are required to estimate power
            LFPower = powerInBand(SpectrumAvg, SpectrumAxis, LFPowerBeg, LFPowerEnd);
        } else {
            LFPower = -1.0f;
        }
        results.add(LFPower);
        Log.i(LOG_TAG + ".Spec", "LF power: "+LFPower);

        float HFPower;
        if ((endSegment-begSegment) > 13.33) {
            HFPower = powerInBand(SpectrumAvg, SpectrumAxis, HFPowerBeg, HFPowerEnd);
        } else {
            HFPower = -1.0f;
        }
        results.add(HFPower);
        Log.i(LOG_TAG + ".Spec", "HF power: "+HFPower);
        Log.i(LOG_TAG + ".Spec", "LF/HF ratio: "+LFPower/HFPower);
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

    /** Make a Hamming window:
      * w(n) = a0 - (1-a0)*cos( 2*PI*n/(N-1) )
      * a0 = 25/46
      * @param windowLength the size of the window
      * @return the hamming window, as a vector of double.
      */
    private double[] makeHammingWindow(int windowLength)
    {
        double[] window = new double[ windowLength ];
        double a0 = 25.0 / 46.0;

        for(int i = 0; i < windowLength; i++) {
            window[ i ] = a0 - ( 1 - a0 ) * Math.cos( 2 * Math.PI * i / ( windowLength - 1 ) );
        }

        return window;
    }

    private float powerInBand(
            final List<Float> SPECTRUM,
            final List<Float> SPECTRUM_AXIS,
            float begFreq, float endFreq)
    {
        final int SPECTRUM_LENGTH = SPECTRUM.size();
        float toret = 0f;

        if ( SPECTRUM_LENGTH > 0 ) {
            for (int index = 0 ; index < SPECTRUM_LENGTH; index++) {
                final float SPECCY_AXIS = SPECTRUM_AXIS.get( index );

                if ( ( SPECCY_AXIS >= begFreq )
                  && ( SPECCY_AXIS <= endFreq ) )
                {
                    toret += SPECTRUM.get( index );
                }
            }

            toret = toret * hammingFactor;
            toret = (float) ( toret / ( 2f * Math.pow( SPECTRUM.size(), 2f ) ) );
        }

        return toret;
    }

    /** @return the MADDRR (median) value. */
    private float calculateMADRR(final List<Float> SIGNAL)
    {
        final int SIGNAL_LEN = SIGNAL.size();
        float toret = 0;

        if ( SIGNAL_LEN > 0 ) {
            final List<Float> DIFS_RR = new ArrayList<>( SIGNAL_LEN - 1 );

            for (int i = 1; i < SIGNAL_LEN; i++) {
                DIFS_RR.add( Math.abs( SIGNAL.get( i ) - SIGNAL.get( i - 1 ) ));
            }

            Collections.sort( DIFS_RR );

            final int LEN_DIFFS_SIGNAL = DIFS_RR.size();
            int n = LEN_DIFFS_SIGNAL / 2;

            if ( LEN_DIFFS_SIGNAL > 0 ) {
                if ( LEN_DIFFS_SIGNAL % 2 == 0 ) {
                    toret = ( DIFS_RR.get( n ) + DIFS_RR.get( n - 1 ) ) / 2;
                } else {
                    toret = DIFS_RR.get( n );
                }
            }
        } else {
            Log.d( LOG_TAG, "MADRR == 0 since #signal == 0" );
        }

        return toret;
    }

    /** @return the entropy. */
    private float calculateApEn(final List<Float> signal, int m, float r)
    {
        r *= _calculateSD(signal);
        return Math.abs( _phi( signal, m + 1, r )  - _phi( signal, m, r ) );
    }

    private float _calculateSD(final List<Float> SIGNAL)
    {
        final int LEN = SIGNAL.size();
        float toret = 0.0f;

        if ( LEN > 0 ) {
            // Calculate mean
            double sum = 0.0f;

            for(float SIGNAL_PART: SIGNAL) {
                sum += SIGNAL_PART;
            }

            final double MEAN = sum / LEN;

            // Calculate std deviation
            double standardDeviation = 0.0f;

            for(final float SIGNAL_PART: SIGNAL) {
                standardDeviation += Math.pow( SIGNAL_PART - MEAN, 2 );
            }

            toret = (float) Math.sqrt( standardDeviation / LEN );
        }

        return toret;
    }

    private float _phi(final List<Float> U, int m, float r)
    {
        final List<List<Float>> X = new ArrayList<>( U.size() / m );
        final int U_SIZE = U.size();
        float toret = 0f;

        if ( U_SIZE > 0 ) {
            for (int i = 0; i < U_SIZE - m + 1 ; ++i) {
                final List<Float> X_ROW = new ArrayList<>( m );

                for (int j = i; j < i + m ; ++j) {
                    X_ROW.add( U.get( j ) );
                }

                X.add( X_ROW );
            }

            final List<Float> C = new ArrayList<>( X.size() );

            for (final List<Float> X_I: X) {
                float C_tmp = .0f;

                for (final List<Float> X_J: X) {
                    if ( _maxdist( X_I, X_J ) <= r ) {
                        C_tmp += 1.0f;
                    }
                }

                C.add( C_tmp / ( U_SIZE - m + 1.0f ) );
            }

            double result = .0f;

            for (float DIST: C) {
                result += Math.log( DIST );
            }

            toret = (float) ( result / ( U_SIZE - m + 1.0f ) );
        }

        return toret;
    }

    private float _maxdist(final List<Float> X_I , final List<Float> X_J)
    {
        final ArrayList<Float> DIFFS = new ArrayList<>( X_I.size() );

        for (int index = 0; index < X_I.size(); index++) {
            DIFFS.add( Math.abs( X_I.get( index ) - X_J.get( index ) ) );
        }

        return Collections.max( DIFFS );
    }

    public int getRRnfCount()
    {
        return this.dataRRnf.size();
    }

    public String[] getEpisodeTypes()
    {
        return this.episodesType.toArray( new String[ 0 ] );
    }

    public Float[] getEpisodeInits()
    {
        return this.episodesInits.toArray( new Float[ 0 ] );
    }

    public Float[] getEpisodeEnds()
    {
        return this.episodesEnds.toArray( new Float[ 0 ] );
    }

    public Float[] getDataHRInterpolated()
    {
        return this.dataHRInterp.toArray( new Float[ 0 ] );
    }

    public Float[] getDataHRInterpolatedForX()
    {
        return this.dataHRInterpX.toArray( new Float[ 0 ] );
    }


    private final List<Float> dataRRnf;
    private final List<Float> episodesInits;
    private final List<Float> episodesEnds;
    private final List<String> episodesType;

    private int numOfTags;
    private int filteredData;

    private List<Float> dataBeatTimes;
    private List<Float> dataRR;
    private List<Float> dataHR;
    private List<Float> dataHRInterpX;
    private List<Float> dataHRInterp;

    private float valueMADRR;
    private float valueApEn;
    private float valuePNN50;
    private float valueSTD;
    private float valueRMS;
    private float valueMeanBPM;

    private final Result result;

    private static final float freq = 4.0f;                   // Interpolation frequency in hz.
    private static final float hammingFactor = 1.586f;

    private static final float totalPowerBeg = 0.0f;
    private static final float totalPowerEnd = 4.0f/2.0f;     // Beginning and end of total power band

    private static final float LFPowerBeg = 0.05f;
    private static final float LFPowerEnd = 0.15f;            // Beginning and end of LF band

    private static final float HFPowerBeg = 0.15f;
    private static final float HFPowerEnd = 0.4f;             // Beginning and end of HF band
}
