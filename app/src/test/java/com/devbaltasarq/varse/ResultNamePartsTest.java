package com.devbaltasarq.varse;


import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.User;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;


public class ResultNamePartsTest {
    private static final long USR_ID = 909090;
    private static final long RES_ID = 787878;
    private static final long EXP_ID = 424242;
    private static final long DURATION = 180;
    private static final String USR_NAME = "hal";
    private static final String EXP_NAME = "manhattan";
    private static final Calendar TIME = Calendar.getInstance();


    @BeforeClass
    public static void init()
    {
        TIME.set( 1974, 7, 11 );
        time = TIME.getTimeInMillis();

        Result res = new Result(
                new Id( RES_ID ),
                time,
                DURATION,
                new User( new Id( USR_ID ), USR_NAME ),
                new Experiment( new Id( EXP_ID ), EXP_NAME ),
                null
        );

        resultName = res.buildResultName();
    }

    @Test
    public void testResultPartUserId()
    {
        Assert.assertEquals(
                USR_ID,
                Result.parseUserIdFromName( resultName )
        );
    }

    @Test
    public void testResultPartTime()
    {
        Assert.assertEquals(
                time,
                Result.parseTimeFromName( resultName )
        );
    }

    private static long time;
    private static String resultName;
}
