package com.devbaltasarq.varse;

import android.support.test.runner.AndroidJUnit4;
import android.util.JsonReader;
import android.util.JsonWriter;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.PictureGroup;
import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.experiment.VideoGroup;

import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

import static org.junit.Assert.*;

/** Tests the JSON conversion mechanisms. */
@RunWith(AndroidJUnit4.class)
public class TestJsonConversionTests {
    @BeforeClass
    public static void init()
    {
        usr1 = new User( Id.createFake(), "Baltasar" );
        expr1 = new Experiment( Id.createFake(), "expr1", true );

        // Experiment 2
        expr2 = new Experiment( Id.createFake(), "expr2", false );
        grp1 = new ManualGroup( Id.createFake(), true, expr2 );
        act1 = new ManualGroup.ManualActivity( Id.createFake(), new Tag( "tag1" ), new Duration( 3 ) );
        grp1.replaceActivities( new ManualGroup.ManualActivity[] { act1 } );
        grp2 = new PictureGroup( Id.createFake(), new Tag( "tag2" ), new Duration( 8 ), false, expr2 );
        grp3 = new VideoGroup( Id.createFake(), new Tag( "tag3" ), true, expr2 );
        act2 = new MediaGroup.MediaActivity( Id.createFake(), new File( "image1.png" ) );
        act3 = new MediaGroup.MediaActivity( Id.createFake(), new File( "video1.ogg" ) );
        grp2.replaceActivities( new MediaGroup.MediaActivity[]{ act2 } );
        grp3.replaceActivities( new MediaGroup.MediaActivity[]{ act3 } );
        expr2.replaceGroups( new Group[] { grp1, grp2, grp3 } );

        // Events
        long time = System.currentTimeMillis();
        event1 = new Result.ActivityChangeEvent( time, act1.getTag().toString() );
        event2 = new Result.BeatEvent( time += 1000, 500 );
        event3 = new Result.ActivityChangeEvent( time += 1000, act2.getTag().toString() );
        event4 = new Result.ActivityChangeEvent( time += 1000, act2.getTag().toString() );

        // Result 1
        res1 = new Result( Id.createFake(), time, 10000, usr1, expr2,
                           new Result.Event[] { event1, event2, event3, event4 } );
    }

    @Test
    public void testBeatEventJsonDump()
    {
        final StringWriter strOut = new StringWriter();
        final String beatEvent1DescFmt = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Orm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        try {
            event2.writeToJSON( new JsonWriter( strOut ) );
            assertEquals( beatEvent1DescFmt.replace( "$1011", Orm.FIELD_EVENT_HEART_BEAT )
                            .replace( "$1012", Long.toString( event2.getMillis() ) )
                            .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) ),
                    strOut.toString() );
        } catch(IOException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testBeatEventJsonLoad()
    {
        final String eventDesc = "{\"" + Orm.FIELD_ELAPSED_TIME + "\":" + event2.getMillis()
                + ",\"" + Orm.FIELD_EVENT_TYPE + "\":\"" + Orm.FIELD_EVENT_HEART_BEAT + "\""
                + ",\"" + Orm.FIELD_HEART_BEAT_AT + "\":" + Long.toString( event2.getTimeOfNewHeartBeat() ) + "}";
        final StringReader strIn = new StringReader( eventDesc );

        try {
            final Result.BeatEvent retrievedEvent = (Result.BeatEvent)
                                            Result.Event.fromJSON( new JsonReader( strIn ) );

            assertEquals( event2, retrievedEvent );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testActivityChangeEvent1JsonDump()
    {
        final StringWriter strOut = new StringWriter();
        final String actChangeEvent1DescFmt = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event1.writeToJSON( new JsonWriter( strOut ) );
            assertEquals( actChangeEvent1DescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event1.getMillis() ) )
                            .replace( "$1023", event1.getTag() ),
                    strOut.toString() );
        } catch(IOException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent1JsonLoad()
    {
        final String eventDesc = "{\"" + Orm.FIELD_ELAPSED_TIME + "\":" + event1.getMillis()
                + ",\"" + Orm.FIELD_EVENT_TYPE + "\":\"" + Orm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Orm.FIELD_TAG + "\":\"" + event1.getTag() + "\"}";
        final StringReader strIn = new StringReader( eventDesc );

        try {
            final Result.ActivityChangeEvent retrievedEvent = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( strIn ) );

            assertEquals( event1, retrievedEvent );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testActivityChange2EventJsonDump()
    {
        final StringWriter strOut = new StringWriter();
        final String actChangeEvent1DescFmt = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event3.writeToJSON( new JsonWriter( strOut ) );
            assertEquals( actChangeEvent1DescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event3.getMillis() ) )
                            .replace( "$1023", event3.getTag() ),
                    strOut.toString() );
        } catch(IOException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent2JsonLoad()
    {
        final String eventDesc = "{\"" + Orm.FIELD_ELAPSED_TIME + "\":" + event3.getMillis()
                + ",\"" + Orm.FIELD_EVENT_TYPE + "\":\"" + Orm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Orm.FIELD_TAG + "\":\"" + event3.getTag() + "\"}";
        final StringReader strIn = new StringReader( eventDesc );

        try {
            final Result.ActivityChangeEvent retrievedEvent = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( strIn ) );

            assertEquals( event3, retrievedEvent );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testActivityChange3EventJsonDump()
    {
        final StringWriter strOut = new StringWriter();
        final String actChangeEvent1DescFmt = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event4.writeToJSON( new JsonWriter( strOut ) );
            assertEquals( actChangeEvent1DescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event4.getMillis() ) )
                            .replace( "$1023", event4.getTag() ),
                    strOut.toString() );
        } catch(IOException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent3JsonLoad()
    {
        final String eventDesc = "{\"" + Orm.FIELD_ELAPSED_TIME + "\":" + event4.getMillis()
                + ",\"" + Orm.FIELD_EVENT_TYPE + "\":\"" + Orm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Orm.FIELD_TAG + "\":\"" + event4.getTag() + "\"}";
        final StringReader strIn = new StringReader( eventDesc );

        try {
            final Result.ActivityChangeEvent retrievedEvent = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( strIn ) );

            assertEquals( event4, retrievedEvent );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testResultJsonDump()
    {
        final StringWriter strOut = new StringWriter();
        final String resultDescFmt = "{\"" + Id.FIELD + "\":$29871,\""
                + Orm.FIELD_TYPE_ID + "\":\"" + res1.getTypeId().toString() + "\","
                + "\"" + Orm.FIELD_EXPERIMENT_ID + "\":$29874,"
                + "\"" + Orm.FIELD_USER_ID + "\":$29875,\""
                + Orm.FIELD_DATE + "\":$29872,"
                + "\"" + Orm.FIELD_EVENTS + "\":[$29873]}";

        final String beatEventDescFmt = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Orm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        final String actChangeEventDescFmt = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        final String eventsDescFmt =
                actChangeEventDescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event1.getMillis() ) )
                        .replace( "$1023", event1.getTag() )
                + "," + beatEventDescFmt.replace( "$1011", Orm.FIELD_EVENT_HEART_BEAT )
                        .replace( "$1012", Long.toString( event2.getMillis() ) )
                        .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) )
                + "," + actChangeEventDescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event3.getMillis() ) )
                        .replace( "$1023", event3.getTag() )
                + "," + actChangeEventDescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event4.getMillis() ) )
                        .replace( "$1023", event4.getTag() );

        try {
            res1.toJSON( strOut );
            assertEquals( resultDescFmt.replace( "$29871", res1.getId().toString() )
                            .replace( "$29872", Long.toString( res1.getTime() ) )
                            .replace( "$29873", eventsDescFmt )
                            .replace( "$29874", res1.getExperiment().getId().toString() )
                            .replace( "$29875", res1.getUser().getId().toString() ),
                    strOut.toString() );
        } catch(JSONException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testResultJsonLoad()
    {
        final String resultDescFmt = "{\"" + Id.FIELD + "\":$29871,\""
                + Orm.FIELD_TYPE_ID + "\":\"" + res1.getTypeId().toString() + "\","
                + "\"" + Orm.FIELD_EXPERIMENT_ID + "\":$29874,"
                + "\"" + Orm.FIELD_USER_ID + "\":$29875,\""
                + Orm.FIELD_DATE + "\":$29872,"
                + "\"" + Orm.FIELD_EVENTS + "\":[$29873]}";

        final String beatEventDescFmt = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Orm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        final String actChangeEventDescFmt = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        final String eventsDescFmt =
                actChangeEventDescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event1.getMillis() ) )
                        .replace( "$1023", event1.getTag() )
                        + "," + beatEventDescFmt.replace( "$1011", Orm.FIELD_EVENT_HEART_BEAT )
                        .replace( "$1012", Long.toString( event2.getMillis() ) )
                        .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) )
                        + "," + actChangeEventDescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event3.getMillis() ) )
                        .replace( "$1023", event3.getTag() )
                        + "," + actChangeEventDescFmt.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event4.getMillis() ) )
                        .replace( "$1023", event4.getTag() );

        final String resDesc = resultDescFmt.replace( "$29871", res1.getId().toString() )
                .replace( "$29872", Long.toString( res1.getTime() ) )
                .replace( "$29873", eventsDescFmt )
                .replace( "$29874", res1.getExperiment().getId().toString() )
                .replace( "$29875", res1.getUser().getId().toString() );

        final StringReader strIn = new StringReader( resDesc );

        try {
            final Result retrievedUsr = Result.fromJSON( strIn );

            assertEquals( res1, retrievedUsr );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testUserJsonDump()
    {
        final StringWriter strOut = new StringWriter();
        final String usrDescFmt = "{\"" + Id.FIELD + "\":$1,\""
                                    + Orm.FIELD_TYPE_ID + "\":\"$87\",\""
                                    + Orm.FIELD_NAME + "\":\"$2\"}";

        try {
            usr1.toJSON( strOut );
            assertEquals( usrDescFmt.replace( "$1", usr1.getId().toString() )
                                .replace( "$87", usr1.getTypeId().toString() )
                                .replace( "$2", usr1.getName() ),
                          strOut.toString() );
        } catch(JSONException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testUserJsonLoad()
    {
        final String usrDesc = "{\"" + Id.FIELD + "\":" + usr1.getId().get()
                + ",\"" + Orm.FIELD_TYPE_ID + "\":\"" + usr1.getTypeId().toString() + "\""
                + ",\"" + Orm.FIELD_NAME + "\":\"" + usr1.getName() + "\"}";
        final StringReader strIn = new StringReader( usrDesc );

        try {
            final User retrievedUsr = User.fromJSON( strIn );

            assertEquals( usr1, retrievedUsr );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testExperiment1JsonDump()
    {
        final StringWriter strOut = new StringWriter();
        final String expr1DescFmt = "{\"" + Id.FIELD + "\":$1,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Orm.FIELD_NAME + "\":\"$2\","
                + "\"" + Orm.FIELD_RANDOM + "\":$3,"
                + "\"" + Orm.FIELD_GROUPS + "\":[]"
                + "}";

        try {
            expr1.toJSON( strOut );
            assertEquals( expr1DescFmt.replace( "$1", expr1.getId().toString() )
                            .replace( "$2", expr1.getName() )
                            .replace( "$87", expr1.getTypeId().toString() )
                            .replace( "$3", Boolean.toString( expr1.isRandom() ) ),
                    strOut.toString() );
        } catch(JSONException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testExperiment1JsonLoad()
    {
        final String expr1Desc = "{\"" + Id.FIELD + "\":" + expr1.getId().get()
                + ",\"" + Orm.FIELD_TYPE_ID + "\":\"" + expr1.getTypeId().toString() + "\""
                + ",\"" + Orm.FIELD_NAME + "\":\"" + expr1.getName() + "\""
                + ",\"" + Orm.FIELD_RANDOM + "\":" + expr1.isRandom()
                + ",\"" + Orm.FIELD_GROUPS + "\":[]}";

        final StringReader strIn = new StringReader( expr1Desc );

        try {
            final Experiment retrievedExperiment = Experiment.fromJSON( strIn );

            assertEquals( expr1, retrievedExperiment );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void textExperiment2JsonLoad()
    {
        // Activity 1
        final String act1DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_TAG + "\":\"$9\","
                + "\"" + Orm.FIELD_TIME + "\":$10}";
        final String act1Desc = act1DescFmt.replace( "$5", Long.toString( act1.getId().get() ) )
                .replace( "$87", act1.getTypeId().toString() )
                .replace( "$9", act1.getTag().toString() )
                .replace( "$10", Integer.toString( act1.getTime().getTimeInSeconds() ) );
        final StringReader strInAct1 = new StringReader( act1Desc );

        try {
            assertEquals( act1, Group.Activity.fromJSON( new JsonReader( strInAct1 ) ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }

        // Activity 2
        final String act2DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_FILE + "\":\"$9\"}";
        final String act2Desc = act2DescFmt.replace( "$5", Long.toString( act2.getId().get() ) )
                .replace( "$87", act2.getTypeId().toString() )
                .replace( "$9", act2.getFile().getName() );
        final StringReader strInAct2 = new StringReader( act2Desc );

        try {
            assertEquals( act2, Group.Activity.fromJSON( new JsonReader( strInAct2 ) ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }

        // Activity 3
        final String act3DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_FILE + "\":\"$9\"}";
        final String act3Desc = act3DescFmt.replace( "$5", Long.toString( act3.getId().get() ) )
                .replace( "$87", act3.getTypeId().toString() )
                .replace( "$9", act3.getFile().getName() );
        final StringReader strInAct3 = new StringReader( act3Desc );

        try {
            assertEquals( act3, Group.Activity.fromJSON( new JsonReader( strInAct3 ) ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }

        // Group 1
        final String grp1DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7]}";
        final String grp1Desc = grp1DescFmt.replace( "$5", Long.toString( grp1.getId().get() ) )
                .replace( "$87", grp1.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp1.isRandom() ) )
                .replace( "$7", act1Desc );
        final StringReader strInGrp1 = new StringReader( grp1Desc );

        try {
            assertEquals( grp1, Group.fromJSON( strInGrp1 ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }


        // Group 2
        final String grp2DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Orm.FIELD_TAG + "\"" + ":\"" + grp2.getTag() + "\","
                + "\"" + Orm.FIELD_TIME + "\"" + ":" + Integer.toString( grp2.getTimeForEachActivity().getTimeInSeconds() )
                + "}";
        final String grp2Desc = grp2DescFmt.replace( "$5", Long.toString( grp2.getId().get() ) )
                .replace( "$87", grp2.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp2.isRandom() ) )
                .replace( "$7", act2Desc );
        final StringReader strInGrp2 = new StringReader( grp2Desc );

        try {
            assertEquals( grp2, Group.fromJSON( strInGrp2 ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }

        // Group 3
        final String grp3DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Orm.FIELD_TAG + "\"" + ":\"" + grp3.getTag() + "\"}";
        final String grp3Desc = grp3DescFmt.replace( "$5", Long.toString( grp3.getId().get() ) )
                .replace( "$87", grp3.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp3.isRandom() ) )
                .replace( "$7", act3Desc );
        final StringReader strInGrp3 = new StringReader( grp3Desc );

        try {
            assertEquals( grp3, Group.fromJSON( strInGrp3 ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }


        final String expr2DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Orm.FIELD_NAME + "\":\"$2\","
                + "\"" + Orm.FIELD_RANDOM + "\":$3,"
                + "\"" + Orm.FIELD_GROUPS + "\":[$41,$42,$43]}";
        final String expr2Desc = expr2DescFmt.replace( "$5", Long.toString( expr2.getId().get() ) )
                .replace( "$87", expr2.getTypeId().toString() )
                .replace( "$2", expr2.getName() )
                .replace( "$3", Boolean.toString( expr2.isRandom() ) )
                .replace( "$41", grp1Desc )
                .replace( "$42", grp2Desc )
                .replace( "$43", grp3Desc );
        final StringReader strInExpr2 = new StringReader( expr2Desc );

        try {
            assertEquals( expr2, Experiment.fromJSON( strInExpr2 ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testExperiment2JsonDump()
    {
        final StringWriter strOut = new StringWriter();
        final String expr2DescFmt = "{\"" + Id.FIELD + "\":$1,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Orm.FIELD_NAME + "\":\"$2\","
                + "\"" + Orm.FIELD_RANDOM + "\":$3,"
                + "\"" + Orm.FIELD_GROUPS + "\":[$41,$42,$43]}";
        final String grp1DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7]}";
        final String grp2DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Orm.FIELD_TAG + "\":\"$TAG\","
                + "\"" + Orm.FIELD_TIME + "\":$TIME}";
        final String grp3DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Orm.FIELD_TAG + "\":\"$TAG\"}";
        final String act1DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_TAG + "\":\"$9\","
                + "\"" + Orm.FIELD_TIME + "\":$10}";
        final String act2DescFmt = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_FILE + "\":\"$9\"}";

        try {
            String expectedJSON = expr2DescFmt.replace( "$1", expr2.getId().toString() )
                    .replace( "$87", expr2.getTypeId().toString() )
                    .replace( "$2", expr2.getName() )
                    .replace( "$3", Boolean.toString( expr2.isRandom() ) );

            // Activity 1
            String act1Desc = act1DescFmt.replace( "$5", Long.toString( act1.getId().get() ) )
                    .replace( "$87", act1.getTypeId().toString() )
                    .replace( "$9", act1.getTag().toString() )
                    .replace( "$10", Integer.toString( act1.getTime().getTimeInSeconds() ) );
            strOut.getBuffer().delete( 0, strOut.getBuffer().length() );
            act1.toJSON( strOut );
            assertEquals( act1Desc, strOut.toString() );

            // Group 1
            String grp1Desc = grp1DescFmt.replace( "$5", Long.toString( grp1.getId().get() ) )
                    .replace( "$87", grp1.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp1.isRandom() ) )
                    .replace( "$7", act1Desc );
            strOut.getBuffer().delete( 0, strOut.getBuffer().length() );
            grp1.toJSON( strOut );
            assertEquals( grp1Desc, strOut.toString() );

            // Activity 2
            String act2Desc = act2DescFmt.replace( "$5", Long.toString( act2.getId().get() ) )
                    .replace( "$87", act2.getTypeId().toString() )
                    .replace( "$9", act2.getFile().getName() );

            strOut.getBuffer().delete( 0, strOut.getBuffer().length() );
            act2.toJSON( strOut );
            assertEquals( act2Desc, strOut.toString() );

            // Activity 3
            String act3Desc = act2DescFmt.replace( "$5", Long.toString( act3.getId().get() ) )
                    .replace( "$87", act3.getTypeId().toString() )
                    .replace( "$9", act3.getFile().getName() );

            strOut.getBuffer().delete( 0, strOut.getBuffer().length() );
            act3.toJSON( strOut );
            assertEquals( act3Desc, strOut.toString() );

            // Group 2
            String grp2Desc = grp2DescFmt.replace( "$5", Long.toString( grp2.getId().get() ) )
                    .replace( "$87", grp2.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp2.isRandom() ) )
                    .replace( "$7", act2Desc )
                    .replace( "$TAG", grp2.getTag().toString() )
                    .replace( "$TIME", Integer.toString( grp2.getTimeForEachActivity().getTimeInSeconds() ) );

            strOut.getBuffer().delete( 0, strOut.getBuffer().length() );
            grp2.toJSON( strOut );
            assertEquals( grp2Desc, strOut.toString() );

            // Group 3
            String grp3Desc = grp3DescFmt.replace( "$5", Long.toString( grp3.getId().get() ) )
                    .replace( "$87", grp3.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp3.isRandom() ) )
                    .replace( "$7", act3Desc )
                    .replace( "$TAG", grp3.getTag().toString() );

            strOut.getBuffer().delete( 0, strOut.getBuffer().length() );
            grp3.toJSON( strOut );
            assertEquals( grp3Desc, strOut.toString() );

            expectedJSON = expectedJSON.replace( "$41", grp1Desc )
                    .replace( "$42", grp2Desc )
                    .replace( "$43", grp3Desc );

            strOut.getBuffer().delete( 0, strOut.getBuffer().length() );
            expr2.toJSON( strOut );
            assertEquals( expectedJSON, strOut.toString() );
        } catch(JSONException exc)
        {
            assertFalse( false );
        }

        return;
    }

    private static Result res1;
    private static Result.ActivityChangeEvent event1;
    private static Result.BeatEvent event2;
    private static Result.ActivityChangeEvent event3;
    private static Result.ActivityChangeEvent event4;

    private static User usr1;
    private static Experiment expr1;
    private static Experiment expr2;
    private static ManualGroup grp1;
    private static PictureGroup grp2;
    private static VideoGroup grp3;
    private static MediaGroup.MediaActivity act2;
    private static MediaGroup.MediaActivity act3;
    private static ManualGroup.ManualActivity act1;
}
