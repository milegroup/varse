package com.devbaltasarq.varse.core.bluetooth;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

class DataFiltering {
    private static void FilterData(ArrayList<Float> dataHR)
    {
        int winlength = 50, minbpm=24, maxbpm=198;
        float last = 13.0f;
   //     ArrayList<float> dataRR = new ArrayList<>( dataRRnf );
        Log.i("XX","I'm going to filter the signal");

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
//                Log.i("XX","Removing beat index "+index);
                dataHR.remove(index);
             //   dataRR.remove(index);
            }
        }

    }
}
