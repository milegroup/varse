package com.devbaltasarq.varse;


import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Result;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;


public class ResultNamePartsTest {
    private static final long RES_ID = 787878;
    private static final long EXP_ID = 424242;
    private static final long DURATION = 180;
    private static final String EXPR_NAME = "manhattan";
    private static final Calendar TIME = Calendar.getInstance();


    @BeforeClass
    public static void init()
    {
        TIME.set( 1974, 7, 11 );
        time = TIME.getTimeInMillis();
        REC = "rr01";

        final Result RES = new Result(
                new Id( RES_ID ),
                time,
                DURATION,
                REC,
                new Experiment( new Id( EXP_ID ), EXPR_NAME ),
                new Result.Event[ 0 ]
        );

        resultName = RES.buildName();
    }

    @Test
    public void testResultPartTime()
    {
        Assert.assertEquals(
                time,
                Result.parseTimeFromName( resultName )
        );
    }

    @Test
    public void testResultPartExprId()
    {
        Assert.assertEquals(
                EXP_ID,
                Result.parseExprIdFromName( resultName )
        );
    }

    @Test
    public void testResultPartName()
    {
        Assert.assertEquals(
                REC,
                Result.parseRecFromName( resultName )
        );
    }

    private static long time;
    private static String resultName;
    private static String REC;
}
