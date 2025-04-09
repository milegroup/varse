package com.devbaltasarq.varse;


import java.util.Calendar;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.ofm.FileManager;


@RunWith(AndroidJUnit4.class)
public class TestResultName {
    @Before
    public void setUp()
    {
        final Calendar DATE_TIME = Calendar.getInstance();

        this.res = new Result(
                Id.create(),
                DATE_TIME.getTimeInMillis(),
                20000,
                "r1",
                new Experiment( Id.create(), "expr1" ),
                new Result.Event[ 0 ]
        );

        return;
    }

    @Test
    public void testName()
    {
        final String NAME = FileManager.buildFileNameFor( this.res );

        Assert.assertEquals( "res", FileManager.extractFileExt( NAME ) );
    }

    private Result res;
}
