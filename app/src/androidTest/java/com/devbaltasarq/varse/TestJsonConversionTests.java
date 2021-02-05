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
        final StringWriter STR_OUT = new StringWriter();
        final String BEAT_EVENT_DESC_FMT = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Orm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        try {
            event2.writeToJSON( new JsonWriter( STR_OUT ) );
            assertEquals( BEAT_EVENT_DESC_FMT.replace( "$1011", Orm.FIELD_EVENT_HEART_BEAT )
                            .replace( "$1012", Long.toString( event2.getMillis() ) )
                            .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) ),
                    STR_OUT.toString() );
        } catch(IOException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testBeatEventJsonLoad()
    {
        final String EVENT_DESC = "{\"" + Orm.FIELD_ELAPSED_TIME + "\":" + event2.getMillis()
                + ",\"" + Orm.FIELD_EVENT_TYPE + "\":\"" + Orm.FIELD_EVENT_HEART_BEAT + "\""
                + ",\"" + Orm.FIELD_HEART_BEAT_AT + "\":" + Long.toString( event2.getTimeOfNewHeartBeat() ) + "}";
        final StringReader STR_IN = new StringReader( EVENT_DESC );

        try {
            final Result.BeatEvent RETRIEVED_EVENT = (Result.BeatEvent)
                                            Result.Event.fromJSON( new JsonReader( STR_IN ) );

            assertEquals( event2, RETRIEVED_EVENT );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testActivityChangeEvent1JsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String ACT_CHANGE_EVENT_DESC_FMT = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event1.writeToJSON( new JsonWriter( STR_OUT ) );
            assertEquals( ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event1.getMillis() ) )
                            .replace( "$1023", event1.getTag() ),
                    STR_OUT.toString() );
        } catch(IOException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent1JsonLoad()
    {
        final String EVENT_DESC = "{\"" + Orm.FIELD_ELAPSED_TIME + "\":" + event1.getMillis()
                + ",\"" + Orm.FIELD_EVENT_TYPE + "\":\"" + Orm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Orm.FIELD_TAG + "\":\"" + event1.getTag() + "\"}";
        final StringReader STR_IN = new StringReader( EVENT_DESC );

        try {
            final Result.ActivityChangeEvent RETRIEVED_EVENT = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( STR_IN ) );

            assertEquals( event1, RETRIEVED_EVENT );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testActivityChange2EventJsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String ACT_CHANGE_EVENT_DESC_FMT = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event3.writeToJSON( new JsonWriter( STR_OUT ) );
            assertEquals( ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event3.getMillis() ) )
                            .replace( "$1023", event3.getTag() ),
                    STR_OUT.toString() );
        } catch(IOException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent2JsonLoad()
    {
        final String EVENT_DESC = "{\"" + Orm.FIELD_ELAPSED_TIME + "\":" + event3.getMillis()
                + ",\"" + Orm.FIELD_EVENT_TYPE + "\":\"" + Orm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Orm.FIELD_TAG + "\":\"" + event3.getTag() + "\"}";
        final StringReader STR_IN = new StringReader( EVENT_DESC );

        try {
            final Result.ActivityChangeEvent RETRIEVED_EVENT = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( STR_IN ) );

            assertEquals( event3, RETRIEVED_EVENT );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testActivityChange3EventJsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String ACT_CHANGE_EVENT_DESC_FORMAT = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event4.writeToJSON( new JsonWriter( STR_OUT ) );
            assertEquals( ACT_CHANGE_EVENT_DESC_FORMAT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event4.getMillis() ) )
                            .replace( "$1023", event4.getTag() ),
                    STR_OUT.toString() );
        } catch(IOException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent3JsonLoad()
    {
        final String EVENT_DESC = "{\"" + Orm.FIELD_ELAPSED_TIME + "\":" + event4.getMillis()
                + ",\"" + Orm.FIELD_EVENT_TYPE + "\":\"" + Orm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Orm.FIELD_TAG + "\":\"" + event4.getTag() + "\"}";
        final StringReader STR_IN = new StringReader( EVENT_DESC );

        try {
            final Result.ActivityChangeEvent RETRIEVED_EVENT = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( STR_IN ) );

            assertEquals( event4, RETRIEVED_EVENT );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testResultJsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String RESULT_DESC_FMT = "{\"" + Id.FIELD + "\":$29871,\""
                + Orm.FIELD_TYPE_ID + "\":\"" + res1.getTypeId().toString() + "\","
                + "\"" + Orm.FIELD_EXPERIMENT_ID + "\":$29874,"
                + "\"" + Orm.FIELD_USER_ID + "\":$29875,\""
                + Orm.FIELD_DATE + "\":$29872,"
                + "\"" + Orm.FIELD_EVENTS + "\":[$29873]}";

        final String BEAT_EVENT_DESC_FMT = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Orm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        final String ACT_CHANGE_EVENT_DESC_FMT = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        final String EVENTS_DESC_FMT =
                ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event1.getMillis() ) )
                        .replace( "$1023", event1.getTag() )
                + "," + BEAT_EVENT_DESC_FMT.replace( "$1011", Orm.FIELD_EVENT_HEART_BEAT )
                        .replace( "$1012", Long.toString( event2.getMillis() ) )
                        .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) )
                + "," + ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event3.getMillis() ) )
                        .replace( "$1023", event3.getTag() )
                + "," + ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event4.getMillis() ) )
                        .replace( "$1023", event4.getTag() );

        try {
            res1.toJSON( STR_OUT );
            assertEquals( RESULT_DESC_FMT.replace( "$29871", res1.getId().toString() )
                            .replace( "$29872", Long.toString( res1.getTime() ) )
                            .replace( "$29873", EVENTS_DESC_FMT )
                            .replace( "$29874", res1.getExperiment().getId().toString() )
                            .replace( "$29875", res1.getUser().getId().toString() ),
                    STR_OUT.toString() );
        } catch(JSONException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testResultJsonLoad()
    {
        final String RESULT_DESC_FMT = "{\"" + Id.FIELD + "\":$29871,\""
                + Orm.FIELD_TYPE_ID + "\":\"" + res1.getTypeId().toString() + "\","
                + "\"" + Orm.FIELD_EXPERIMENT_ID + "\":$29874,"
                + "\"" + Orm.FIELD_USER_ID + "\":$29875,\""
                + Orm.FIELD_DATE + "\":$29872,"
                + "\"" + Orm.FIELD_EVENTS + "\":[$29873]}";

        final String BEAT_EVENTS_DESC_FMT = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Orm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        final String ACT_CHANGE_EVENT_DESC_FMT = "{\""
                + Orm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Orm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Orm.FIELD_TAG + "\":\"$1023\"}";

        final String EVENTS_DESC_FMT =
                ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event1.getMillis() ) )
                        .replace( "$1023", event1.getTag() )
                        + "," + BEAT_EVENTS_DESC_FMT.replace( "$1011", Orm.FIELD_EVENT_HEART_BEAT )
                        .replace( "$1012", Long.toString( event2.getMillis() ) )
                        .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) )
                        + "," + ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event3.getMillis() ) )
                        .replace( "$1023", event3.getTag() )
                        + "," + ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Orm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event4.getMillis() ) )
                        .replace( "$1023", event4.getTag() );

        final String RES_DESC = RESULT_DESC_FMT.replace( "$29871", res1.getId().toString() )
                .replace( "$29872", Long.toString( res1.getTime() ) )
                .replace( "$29873", EVENTS_DESC_FMT )
                .replace( "$29874", res1.getExperiment().getId().toString() )
                .replace( "$29875", res1.getUser().getId().toString() );

        final StringReader STR_IN = new StringReader( RES_DESC );

        try {
            final Result RETRIEVED_USR = Result.fromJSON( STR_IN );

            assertEquals( res1, RETRIEVED_USR );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testUserJsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String USR_DESC_FMT = "{\"" + Id.FIELD + "\":$1,\""
                                    + Orm.FIELD_TYPE_ID + "\":\"$87\",\""
                                    + Orm.FIELD_NAME + "\":\"$2\"}";

        try {
            usr1.toJSON( STR_OUT );
            assertEquals( USR_DESC_FMT.replace( "$1", usr1.getId().toString() )
                                .replace( "$87", usr1.getTypeId().toString() )
                                .replace( "$2", usr1.getName() ),
                          STR_OUT.toString() );
        } catch(JSONException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testUserJsonLoad()
    {
        final String USR_DESC = "{\"" + Id.FIELD + "\":" + usr1.getId().get()
                + ",\"" + Orm.FIELD_TYPE_ID + "\":\"" + usr1.getTypeId().toString() + "\""
                + ",\"" + Orm.FIELD_NAME + "\":\"" + usr1.getName() + "\"}";
        final StringReader STR_IN = new StringReader( USR_DESC );

        try {
            final User RETRIEVED_USR = User.fromJSON( STR_IN );

            assertEquals( usr1, RETRIEVED_USR );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testExperiment1JsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String EXPR_DESC_FMT = "{\"" + Id.FIELD + "\":$1,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Orm.FIELD_NAME + "\":\"$2\","
                + "\"" + Orm.FIELD_RANDOM + "\":$3,"
                + "\"" + Orm.FIELD_GROUPS + "\":[]"
                + "}";

        try {
            expr1.toJSON( STR_OUT );
            assertEquals( EXPR_DESC_FMT.replace( "$1", expr1.getId().toString() )
                            .replace( "$2", expr1.getName() )
                            .replace( "$87", expr1.getTypeId().toString() )
                            .replace( "$3", Boolean.toString( expr1.isRandom() ) ),
                    STR_OUT.toString() );
        } catch(JSONException exc)
        {
            assertFalse( false );
        }

        return;
    }

    @Test
    public void testExperiment1JsonLoad()
    {
        final String EXPR_DESC = "{\"" + Id.FIELD + "\":" + expr1.getId().get()
                + ",\"" + Orm.FIELD_TYPE_ID + "\":\"" + expr1.getTypeId().toString() + "\""
                + ",\"" + Orm.FIELD_NAME + "\":\"" + expr1.getName() + "\""
                + ",\"" + Orm.FIELD_RANDOM + "\":" + expr1.isRandom()
                + ",\"" + Orm.FIELD_GROUPS + "\":[]}";

        final StringReader STR_IN = new StringReader( EXPR_DESC );

        try {
            final Experiment RETRIEVED_EXPERIMENT = Experiment.fromJSON( STR_IN );

            assertEquals( expr1, RETRIEVED_EXPERIMENT );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void textExperiment2JsonLoad()
    {
        // Activity 1
        final String ACT1_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_TAG + "\":\"$9\","
                + "\"" + Orm.FIELD_TIME + "\":$10}";
        final String ACT1_DESC = ACT1_DESC_FMT.replace( "$5", Long.toString( act1.getId().get() ) )
                .replace( "$87", act1.getTypeId().toString() )
                .replace( "$9", act1.getTag().toString() )
                .replace( "$10", Integer.toString( act1.getTime().getTimeInSeconds() ) );
        final StringReader STR_IN_ACT1 = new StringReader( ACT1_DESC );

        try {
            assertEquals( act1, Group.Activity.fromJSON( new JsonReader( STR_IN_ACT1 ) ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }

        // Activity 2
        final String ACT2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_FILE + "\":\"$9\"}";
        final String ACT2_DESC = ACT2_DESC_FMT.replace( "$5", Long.toString( act2.getId().get() ) )
                .replace( "$87", act2.getTypeId().toString() )
                .replace( "$9", act2.getFile().getName() );
        final StringReader STR_IN_ACT2 = new StringReader( ACT2_DESC );

        try {
            assertEquals( act2, Group.Activity.fromJSON( new JsonReader( STR_IN_ACT2 ) ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }

        // Activity 3
        final String ACT3_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_FILE + "\":\"$9\"}";
        final String ACT3_DESC = ACT3_DESC_FMT.replace( "$5", Long.toString( act3.getId().get() ) )
                .replace( "$87", act3.getTypeId().toString() )
                .replace( "$9", act3.getFile().getName() );
        final StringReader STR_IN_ACT3 = new StringReader( ACT3_DESC );

        try {
            assertEquals( act3, Group.Activity.fromJSON( new JsonReader( STR_IN_ACT3 ) ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }

        // Group 1
        final String GRP1_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7]}";
        final String GRP1_DESC = GRP1_DESC_FMT.replace( "$5", Long.toString( grp1.getId().get() ) )
                .replace( "$87", grp1.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp1.isRandom() ) )
                .replace( "$7", ACT1_DESC );
        final StringReader STR_IN_GRP1 = new StringReader( GRP1_DESC );

        try {
            assertEquals( grp1, Group.fromJSON( STR_IN_GRP1 ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }


        // Group 2
        final String GRP2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Orm.FIELD_TAG + "\"" + ":\"" + grp2.getTag() + "\","
                + "\"" + Orm.FIELD_TIME + "\"" + ":" + Integer.toString( grp2.getTimeForEachActivity().getTimeInSeconds() )
                + "}";
        final String GRP2_DESC = GRP2_DESC_FMT.replace( "$5", Long.toString( grp2.getId().get() ) )
                .replace( "$87", grp2.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp2.isRandom() ) )
                .replace( "$7", ACT2_DESC );
        final StringReader STR_IN_GRP2 = new StringReader( GRP2_DESC );

        try {
            assertEquals( grp2, Group.fromJSON( STR_IN_GRP2 ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }

        // Group 3
        final String GRP3_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Orm.FIELD_TAG + "\"" + ":\"" + grp3.getTag() + "\"}";
        final String GRP3_DESC = GRP3_DESC_FMT.replace( "$5", Long.toString( grp3.getId().get() ) )
                .replace( "$87", grp3.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp3.isRandom() ) )
                .replace( "$7", ACT3_DESC );
        final StringReader STR_IN_GRP3 = new StringReader( GRP3_DESC );

        try {
            assertEquals( grp3, Group.fromJSON( STR_IN_GRP3 ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }


        final String EXPR2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Orm.FIELD_NAME + "\":\"$2\","
                + "\"" + Orm.FIELD_RANDOM + "\":$3,"
                + "\"" + Orm.FIELD_GROUPS + "\":[$41,$42,$43]}";
        final String EXPR2_DESC = EXPR2_DESC_FMT.replace( "$5", Long.toString( expr2.getId().get() ) )
                .replace( "$87", expr2.getTypeId().toString() )
                .replace( "$2", expr2.getName() )
                .replace( "$3", Boolean.toString( expr2.isRandom() ) )
                .replace( "$41", GRP1_DESC )
                .replace( "$42", GRP2_DESC )
                .replace( "$43", GRP3_DESC );
        final StringReader STR_IN_EXPR2 = new StringReader( EXPR2_DESC );

        try {
            assertEquals( expr2, Experiment.fromJSON( STR_IN_EXPR2 ) );
        } catch(JSONException exc)
        {
            assertFalse( exc.getMessage(), true );
        }
    }

    @Test
    public void testExperiment2JsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String EXPR2_DESC_FMT = "{\"" + Id.FIELD + "\":$1,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Orm.FIELD_NAME + "\":\"$2\","
                + "\"" + Orm.FIELD_RANDOM + "\":$3,"
                + "\"" + Orm.FIELD_GROUPS + "\":[$41,$42,$43]}";
        final String GRP1_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7]}";
        final String GRP2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Orm.FIELD_TAG + "\":\"$TAG\","
                + "\"" + Orm.FIELD_TIME + "\":$TIME}";
        final String GRP3_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_RANDOM + "\":$6,"
                + "\"" + Orm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Orm.FIELD_TAG + "\":\"$TAG\"}";
        final String ACT1_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_TAG + "\":\"$9\","
                + "\"" + Orm.FIELD_TIME + "\":$10}";
        final String ACT2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Orm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Orm.FIELD_FILE + "\":\"$9\"}";

        try {
            String expectedJSON = EXPR2_DESC_FMT.replace( "$1", expr2.getId().toString() )
                    .replace( "$87", expr2.getTypeId().toString() )
                    .replace( "$2", expr2.getName() )
                    .replace( "$3", Boolean.toString( expr2.isRandom() ) );

            // Activity 1
            String act1Desc = ACT1_DESC_FMT.replace( "$5", Long.toString( act1.getId().get() ) )
                    .replace( "$87", act1.getTypeId().toString() )
                    .replace( "$9", act1.getTag().toString() )
                    .replace( "$10", Integer.toString( act1.getTime().getTimeInSeconds() ) );
            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            act1.toJSON( STR_OUT );
            assertEquals( act1Desc, STR_OUT.toString() );

            // Group 1
            String grp1Desc = GRP1_DESC_FMT.replace( "$5", Long.toString( grp1.getId().get() ) )
                    .replace( "$87", grp1.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp1.isRandom() ) )
                    .replace( "$7", act1Desc );
            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            grp1.toJSON( STR_OUT );
            assertEquals( grp1Desc, STR_OUT.toString() );

            // Activity 2
            String act2Desc = ACT2_DESC_FMT.replace( "$5", Long.toString( act2.getId().get() ) )
                    .replace( "$87", act2.getTypeId().toString() )
                    .replace( "$9", act2.getFile().getName() );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            act2.toJSON( STR_OUT );
            assertEquals( act2Desc, STR_OUT.toString() );

            // Activity 3
            String act3Desc = ACT2_DESC_FMT.replace( "$5", Long.toString( act3.getId().get() ) )
                    .replace( "$87", act3.getTypeId().toString() )
                    .replace( "$9", act3.getFile().getName() );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            act3.toJSON( STR_OUT );
            assertEquals( act3Desc, STR_OUT.toString() );

            // Group 2
            String grp2Desc = GRP2_DESC_FMT.replace( "$5", Long.toString( grp2.getId().get() ) )
                    .replace( "$87", grp2.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp2.isRandom() ) )
                    .replace( "$7", act2Desc )
                    .replace( "$TAG", grp2.getTag().toString() )
                    .replace( "$TIME", Integer.toString( grp2.getTimeForEachActivity().getTimeInSeconds() ) );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            grp2.toJSON( STR_OUT );
            assertEquals( grp2Desc, STR_OUT.toString() );

            // Group 3
            String grp3Desc = GRP3_DESC_FMT.replace( "$5", Long.toString( grp3.getId().get() ) )
                    .replace( "$87", grp3.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp3.isRandom() ) )
                    .replace( "$7", act3Desc )
                    .replace( "$TAG", grp3.getTag().toString() );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            grp3.toJSON( STR_OUT );
            assertEquals( grp3Desc, STR_OUT.toString() );

            expectedJSON = expectedJSON.replace( "$41", grp1Desc )
                    .replace( "$42", grp2Desc )
                    .replace( "$43", grp3Desc );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            expr2.toJSON( STR_OUT );
            assertEquals( expectedJSON, STR_OUT.toString() );
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
