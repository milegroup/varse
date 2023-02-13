package com.devbaltasarq.varse;

import com.devbaltasarq.varse.core.Duration;
import com.devbaltasarq.varse.core.Experiment;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.PlainStringEncoder;
import com.devbaltasarq.varse.core.Result;
import com.devbaltasarq.varse.core.experiment.Group;
import com.devbaltasarq.varse.core.experiment.ManualGroup;
import com.devbaltasarq.varse.core.experiment.MediaGroup;
import com.devbaltasarq.varse.core.experiment.PictureGroup;
import com.devbaltasarq.varse.core.experiment.Tag;
import com.devbaltasarq.varse.core.experiment.VideoGroup;
import com.devbaltasarq.varse.core.ofmcache.EntitiesCache;

import android.util.JsonReader;
import android.util.JsonWriter;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;


/** Tests the JSON conversion mechanisms. */
public class TestJsonConversionTests {
    @BeforeClass
    public static void init()
    {
        final ArrayList<Group<? extends Group.Activity>> GRPS = new ArrayList<>( 3 );
        Ofm.init( ApplicationProvider.getApplicationContext(),
                    PlainStringEncoder.get() );

        expr1 = new Experiment( Id.createFake(), "expr1", true );

        // Experiment 2
        expr2 = new Experiment( Id.create(), "expr2", false );
        grp1 = new ManualGroup( Id.createFake(), true, expr2 );
        act1 = new ManualGroup.ManualActivity( Id.createFake(), new Tag( "tag1" ), new Duration( 3 ) );
        grp1.replaceActivities( new ManualGroup.ManualActivity[] { act1 } );
        grp2 = new PictureGroup( Id.createFake(), new Tag( "tag2" ), new Duration( 8 ), false, expr2 );
        grp3 = new VideoGroup( Id.createFake(), new Tag( "tag3" ), true, expr2 );
        act2 = new MediaGroup.MediaActivity( Id.createFake(), new File( "image1.png" ) );
        act3 = new MediaGroup.MediaActivity( Id.createFake(), new File( "video1.ogg" ) );
        grp2.replaceActivities( new MediaGroup.MediaActivity[]{ act2 } );
        grp3.replaceActivities( new MediaGroup.MediaActivity[]{ act3 } );

        GRPS.add( grp1 );
        GRPS.add( grp2 );
        GRPS.add( grp3 );
        expr2.replaceGroups( GRPS );

        // Events
        long time = System.currentTimeMillis();
        event1 = new Result.ActivityChangeEvent( time, act1.getTag() );
        event2 = new Result.BeatEvent( time += 1000, 500 );
        event3 = new Result.ActivityChangeEvent( time += 1000, act2.getTag() );
        event4 = new Result.ActivityChangeEvent( time += 1000, act2.getTag() );

        // Result 1
        res1 = new Result( Id.createFake(), time, 10000, "rec_tests", expr2,
                           new Result.Event[] { event1, event2, event3, event4 } );
    }

    @Test
    public void testBeatEventJsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String BEAT_EVENT_DESC_FMT = "{\""
                + Ofm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Ofm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Ofm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        try {
            event2.writeToJSON( new JsonWriter( STR_OUT ) );
            Assert.assertEquals( BEAT_EVENT_DESC_FMT.replace( "$1011", Ofm.FIELD_EVENT_HEART_BEAT )
                            .replace( "$1012", Long.toString( event2.getMillis() ) )
                            .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) ),
                    STR_OUT.toString() );
        } catch(IOException exc)
        {
            Assert.fail( exc.getMessage() );
        }

        return;
    }

    @Test
    public void testBeatEventJsonLoad()
    {
        final String EVENT_DESC = "{\"" + Ofm.FIELD_ELAPSED_TIME + "\":" + event2.getMillis()
                + ",\"" + Ofm.FIELD_EVENT_TYPE + "\":\"" + Ofm.FIELD_EVENT_HEART_BEAT + "\""
                + ",\"" + Ofm.FIELD_HEART_BEAT_AT + "\":" + event2.getTimeOfNewHeartBeat() + "}";
        final StringReader STR_IN = new StringReader( EVENT_DESC );

        try {
            final Result.BeatEvent RETRIEVED_EVENT = (Result.BeatEvent)
                                            Result.Event.fromJSON( new JsonReader( STR_IN ) );

            Assert.assertEquals( event2, RETRIEVED_EVENT );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }
    }

    @Test
    public void testActivityChangeEvent1JsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String ACT_CHANGE_EVENT_DESC_FMT = "{\""
                + Ofm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Ofm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Ofm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event1.writeToJSON( new JsonWriter( STR_OUT ) );
            Assert.assertEquals( ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event1.getMillis() ) )
                            .replace( "$1023", event1.getTag().toString() ),
                    STR_OUT.toString() );
        } catch(IOException exc)
        {
            Assert.assertFalse( false );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent1JsonLoad()
    {
        final String EVENT_DESC = "{\"" + Ofm.FIELD_ELAPSED_TIME + "\":" + event1.getMillis()
                + ",\"" + Ofm.FIELD_EVENT_TYPE + "\":\"" + Ofm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Ofm.FIELD_TAG + "\":\"" + event1.getTag() + "\"}";
        final StringReader STR_IN = new StringReader( EVENT_DESC );

        try {
            final Result.ActivityChangeEvent RETRIEVED_EVENT = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( STR_IN ) );

            Assert.assertEquals( event1, RETRIEVED_EVENT );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }
    }

    @Test
    public void testActivityChange2EventJsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String ACT_CHANGE_EVENT_DESC_FMT = "{\""
                + Ofm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Ofm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Ofm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event3.writeToJSON( new JsonWriter( STR_OUT ) );
            Assert.assertEquals( ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event3.getMillis() ) )
                            .replace( "$1023", event3.getTag().toString() ),
                    STR_OUT.toString() );
        } catch(IOException exc)
        {
            Assert.assertFalse( false );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent2JsonLoad()
    {
        final String EVENT_DESC = "{\"" + Ofm.FIELD_ELAPSED_TIME + "\":" + event3.getMillis()
                + ",\"" + Ofm.FIELD_EVENT_TYPE + "\":\"" + Ofm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Ofm.FIELD_TAG + "\":\"" + event3.getTag() + "\"}";
        final StringReader STR_IN = new StringReader( EVENT_DESC );

        try {
            final Result.ActivityChangeEvent RETRIEVED_EVENT = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( STR_IN ) );

            Assert.assertEquals( event3, RETRIEVED_EVENT );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }
    }

    @Test
    public void testActivityChange3EventJsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String ACT_CHANGE_EVENT_DESC_FORMAT = "{\""
                + Ofm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Ofm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Ofm.FIELD_TAG + "\":\"$1023\"}";

        try {
            event4.writeToJSON( new JsonWriter( STR_OUT ) );
            Assert.assertEquals( ACT_CHANGE_EVENT_DESC_FORMAT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                            .replace( "$1022", Long.toString( event4.getMillis() ) )
                            .replace( "$1023", event4.getTag().toString() ),
                    STR_OUT.toString() );
        } catch(IOException exc)
        {
            Assert.fail( exc.getMessage() );
        }

        return;
    }

    @Test
    public void testActivityChangeEvent3JsonLoad()
    {
        final String EVENT_DESC = "{\"" + Ofm.FIELD_ELAPSED_TIME + "\":" + event4.getMillis()
                + ",\"" + Ofm.FIELD_EVENT_TYPE + "\":\"" + Ofm.FIELD_EVENT_ACTIVITY_CHANGE + "\""
                + ",\"" + Ofm.FIELD_TAG + "\":\"" + event4.getTag() + "\"}";
        final StringReader STR_IN = new StringReader( EVENT_DESC );

        try {
            final Result.ActivityChangeEvent RETRIEVED_EVENT = (Result.ActivityChangeEvent)
                    Result.Event.fromJSON( new JsonReader( STR_IN ) );

            Assert.assertEquals( event4, RETRIEVED_EVENT );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }
    }

    @Test
    public void testResultJsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String RESULT_DESC_FMT = "{\"" + Id.FIELD + "\":$29871,\""
                + Ofm.FIELD_TYPE_ID + "\":\"" + res1.getTypeId().toString() + "\","
                + "\"" + Ofm.FIELD_NAME + "\":\"$29876\","
                + "\"" + Ofm.FIELD_DATE + "\":$29872,"
                + "\"" + Ofm.FIELD_TIME + "\":$29877,"
                + "\"" + EntitiesCache.FIELD_EXPERIMENT_ID + "\":$29874,"
                + "\"" + Ofm.FIELD_REC + "\":\"$29875\","
                + "\"" + Ofm.FIELD_EVENTS + "\":[$29873]}";

        final String BEAT_EVENT_DESC_FMT = "{\""
                + Ofm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Ofm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Ofm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        final String ACT_CHANGE_EVENT_DESC_FMT = "{\""
                + Ofm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Ofm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Ofm.FIELD_TAG + "\":\"$1023\"}";

        final String EVENTS_DESC_FMT =
                ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event1.getMillis() ) )
                        .replace( "$1023", event1.getTag().toString() )
                + "," + BEAT_EVENT_DESC_FMT.replace( "$1011", Ofm.FIELD_EVENT_HEART_BEAT )
                        .replace( "$1012", Long.toString( event2.getMillis() ) )
                        .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) )
                + "," + ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event3.getMillis() ) )
                        .replace( "$1023", event3.getTag().toString() )
                + "," + ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event4.getMillis() ) )
                        .replace( "$1023", event4.getTag().toString() );

        try {
            final String EXPECTED_JSON = RESULT_DESC_FMT.replace( "$29871", res1.getId().toString() )
                    .replace( "$29872", Long.toString( res1.getTime() ) )
                    .replace( "$29873", EVENTS_DESC_FMT )
                    .replace( "$29874", res1.getExperiment().getId().toString() )
                    .replace( "$29875", res1.getRec() )
                    .replace( "$29876", res1.buildResultName() )
                    .replace( "$29877", Long.toString( res1.getDurationInMillis() ) );

            res1.toJSON( STR_OUT );
            Assert.assertEquals( EXPECTED_JSON,
                    STR_OUT.toString() );
        } catch(JSONException exc)
        {
            Assert.assertFalse( false );
        }

        return;
    }

    @Test
    public void testResultJsonLoad()
    {
        final String RESULT_DESC_FMT = "{\"" + Id.FIELD + "\":$29871,\""
                + Ofm.FIELD_TYPE_ID + "\":\"" + res1.getTypeId().toString() + "\","
                + "\"" + Ofm.FIELD_NAME + "\":\"$29876\","
                + "\"" + Ofm.FIELD_DATE + "\":$29872,"
                + "\"" + Ofm.FIELD_TIME + "\":$29877,"
                + "\"" + EntitiesCache.FIELD_EXPERIMENT_ID + "\":$29874,"
                + "\"" + Ofm.FIELD_REC + "\":\"$29875\","
                + "\"" + Ofm.FIELD_EVENTS + "\":[$29873]}";

        final String BEAT_EVENTS_DESC_FMT = "{\""
                + Ofm.FIELD_ELAPSED_TIME + "\":$1012,\""
                + Ofm.FIELD_EVENT_TYPE + "\":\"$1011\",\""
                + Ofm.FIELD_HEART_BEAT_AT + "\":\"$1013\"}";

        final String ACT_CHANGE_EVENT_DESC_FMT = "{\""
                + Ofm.FIELD_ELAPSED_TIME + "\":$1022,\""
                + Ofm.FIELD_EVENT_TYPE + "\":\"$1021\",\""
                + Ofm.FIELD_TAG + "\":\"$1023\"}";

        final String EVENTS_DESC_FMT =
                ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event1.getMillis() ) )
                        .replace( "$1023", event1.getTag().toString() )
                        + "," + BEAT_EVENTS_DESC_FMT.replace( "$1011", Ofm.FIELD_EVENT_HEART_BEAT )
                        .replace( "$1012", Long.toString( event2.getMillis() ) )
                        .replace( "$1013", Long.toString( event2.getTimeOfNewHeartBeat() ) )
                        + "," + ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event3.getMillis() ) )
                        .replace( "$1023", event3.getTag().toString() )
                        + "," + ACT_CHANGE_EVENT_DESC_FMT.replace( "$1021", Ofm.FIELD_EVENT_ACTIVITY_CHANGE )
                        .replace( "$1022", Long.toString( event4.getMillis() ) )
                        .replace( "$1023", event4.getTag().toString() );

        final String RES_DESC = RESULT_DESC_FMT.replace( "$29871", res1.getId().toString() )
                .replace( "$29872", Long.toString( res1.getTime() ) )
                .replace( "$29873", EVENTS_DESC_FMT )
                .replace( "$29874", res1.getExperiment().getId().toString() )
                .replace( "$29875", res1.getRec() )
                .replace( "$29876", res1.buildResultName() )
                .replace( "$29877", Long.toString( res1.getDurationInMillis() ) );

        final StringReader STR_IN = new StringReader( RES_DESC );

        try {
            Ofm.get().store( expr2 );
        } catch(IOException exc) {
            Assert.fail( exc.getMessage() );
        }

        try {
            final Result RETRIEVED_RES = Result.fromJSON( STR_IN );

            Assert.assertEquals( res1, RETRIEVED_RES );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }
    }

    @Test
    public void testExperiment1JsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String EXPR_DESC_FMT = "{\"" + Id.FIELD + "\":$1,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Ofm.FIELD_NAME + "\":\"$2\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$3,"
                + "\"" + Ofm.FIELD_GROUPS + "\":[]"
                + "}";

        try {
            expr1.toJSON( STR_OUT );
            Assert.assertEquals( EXPR_DESC_FMT.replace( "$1", expr1.getId().toString() )
                            .replace( "$2", expr1.getName() )
                            .replace( "$87", expr1.getTypeId().toString() )
                            .replace( "$3", Boolean.toString( expr1.isRandom() ) ),
                    STR_OUT.toString() );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }

        return;
    }

    @Test
    public void testExperiment1JsonLoad()
    {
        final String EXPR_DESC = "{\"" + Id.FIELD + "\":" + expr1.getId().get()
                + ",\"" + Ofm.FIELD_TYPE_ID + "\":\"" + expr1.getTypeId().toString() + "\""
                + ",\"" + Ofm.FIELD_NAME + "\":\"" + expr1.getName() + "\""
                + ",\"" + Ofm.FIELD_RANDOM + "\":" + expr1.isRandom()
                + ",\"" + Ofm.FIELD_GROUPS + "\":[]}";

        final StringReader STR_IN = new StringReader( EXPR_DESC );

        try {
            final Experiment RETRIEVED_EXPERIMENT = Experiment.fromJSON( STR_IN );

            Assert.assertEquals( expr1, RETRIEVED_EXPERIMENT );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }
    }

    @Test
    public void textExperiment2JsonLoad()
    {
        // Activity 1
        final String ACT1_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_TAG + "\":\"$9\","
                + "\"" + Ofm.FIELD_TIME + "\":$10}";
        final String ACT1_DESC = ACT1_DESC_FMT.replace( "$5", Long.toString( act1.getId().get() ) )
                .replace( "$87", act1.getTypeId().toString() )
                .replace( "$9", act1.getTag().toString() )
                .replace( "$10", Integer.toString( act1.getTime().getTimeInSeconds() ) );
        final StringReader STR_IN_ACT1 = new StringReader( ACT1_DESC );

        try {
            Assert.assertEquals( act1, Group.Activity.fromJSON( new JsonReader( STR_IN_ACT1 ) ) );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }

        // Activity 2
        final String ACT2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_FILE + "\":\"$9\"}";
        final String ACT2_DESC = ACT2_DESC_FMT.replace( "$5", Long.toString( act2.getId().get() ) )
                .replace( "$87", act2.getTypeId().toString() )
                .replace( "$9", act2.getFile().getName() );
        final StringReader STR_IN_ACT2 = new StringReader( ACT2_DESC );

        try {
            Assert.assertEquals( act2, Group.Activity.fromJSON( new JsonReader( STR_IN_ACT2 ) ) );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }

        // Activity 3
        final String ACT3_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_FILE + "\":\"$9\"}";
        final String ACT3_DESC = ACT3_DESC_FMT.replace( "$5", Long.toString( act3.getId().get() ) )
                .replace( "$87", act3.getTypeId().toString() )
                .replace( "$9", act3.getFile().getName() );
        final StringReader STR_IN_ACT3 = new StringReader( ACT3_DESC );

        try {
            Assert.assertEquals( act3, Group.Activity.fromJSON( new JsonReader( STR_IN_ACT3 ) ) );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }

        // Group 1
        final String GRP1_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$6,"
                + "\"" + Ofm.FIELD_ACTIVITIES + "\"" + ":[$7]}";
        final String GRP1_DESC = GRP1_DESC_FMT.replace( "$5", Long.toString( grp1.getId().get() ) )
                .replace( "$87", grp1.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp1.isRandom() ) )
                .replace( "$7", ACT1_DESC );
        final StringReader STR_IN_GRP1 = new StringReader( GRP1_DESC );

        try {
            Assert.assertEquals( grp1, Group.fromJSON( STR_IN_GRP1 ) );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }


        // Group 2
        final String GRP2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$6,"
                + "\"" + Ofm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Ofm.FIELD_TAG + "\"" + ":\"" + grp2.getTag() + "\","
                + "\"" + Ofm.FIELD_TIME + "\"" + ":" + grp2.getTimeForEachActivity().getTimeInSeconds()
                + "}";
        final String GRP2_DESC = GRP2_DESC_FMT.replace( "$5", Long.toString( grp2.getId().get() ) )
                .replace( "$87", grp2.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp2.isRandom() ) )
                .replace( "$7", ACT2_DESC );
        final StringReader STR_IN_GRP2 = new StringReader( GRP2_DESC );

        try {
            Assert.assertEquals( grp2, Group.fromJSON( STR_IN_GRP2 ) );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }

        // Group 3
        final String GRP3_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$6,"
                + "\"" + Ofm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Ofm.FIELD_TAG + "\"" + ":\"" + grp3.getTag() + "\"}";
        final String GRP3_DESC = GRP3_DESC_FMT.replace( "$5", Long.toString( grp3.getId().get() ) )
                .replace( "$87", grp3.getTypeId().toString() )
                .replace( "$6", Boolean.toString( grp3.isRandom() ) )
                .replace( "$7", ACT3_DESC );
        final StringReader STR_IN_GRP3 = new StringReader( GRP3_DESC );

        try {
            Assert.assertEquals( grp3, Group.fromJSON( STR_IN_GRP3 ) );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }


        final String EXPR2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Ofm.FIELD_NAME + "\":\"$2\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$3,"
                + "\"" + Ofm.FIELD_GROUPS + "\":[$41,$42,$43]}";
        final String EXPR2_DESC = EXPR2_DESC_FMT.replace( "$5", Long.toString( expr2.getId().get() ) )
                .replace( "$87", expr2.getTypeId().toString() )
                .replace( "$2", expr2.getName() )
                .replace( "$3", Boolean.toString( expr2.isRandom() ) )
                .replace( "$41", GRP1_DESC )
                .replace( "$42", GRP2_DESC )
                .replace( "$43", GRP3_DESC );
        final StringReader STR_IN_EXPR2 = new StringReader( EXPR2_DESC );

        try {
            Assert.assertEquals( expr2, Experiment.fromJSON( STR_IN_EXPR2 ) );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }
    }

    @Test
    public void testExperiment2JsonDump()
    {
        final StringWriter STR_OUT = new StringWriter();
        final String EXPR2_DESC_FMT = "{\"" + Id.FIELD + "\":$1,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\",\""
                + Ofm.FIELD_NAME + "\":\"$2\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$3,"
                + "\"" + Ofm.FIELD_GROUPS + "\":[$41,$42,$43]}";
        final String GRP1_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$6,"
                + "\"" + Ofm.FIELD_ACTIVITIES + "\"" + ":[$7]}";
        final String GRP2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$6,"
                + "\"" + Ofm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Ofm.FIELD_TAG + "\":\"$TAG\","
                + "\"" + Ofm.FIELD_TIME + "\":$TIME}";
        final String GRP3_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_RANDOM + "\":$6,"
                + "\"" + Ofm.FIELD_ACTIVITIES + "\"" + ":[$7],"
                + "\"" + Ofm.FIELD_TAG + "\":\"$TAG\"}";
        final String ACT1_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_TAG + "\":\"$9\","
                + "\"" + Ofm.FIELD_TIME + "\":$10}";
        final String ACT2_DESC_FMT = "{\"" + Id.FIELD + "\":$5,\""
                + Ofm.FIELD_TYPE_ID + "\":\"$87\","
                + "\"" + Ofm.FIELD_FILE + "\":\"$9\"}";

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
            Assert.assertEquals( act1Desc, STR_OUT.toString() );

            // Group 1
            String grp1Desc = GRP1_DESC_FMT.replace( "$5", Long.toString( grp1.getId().get() ) )
                    .replace( "$87", grp1.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp1.isRandom() ) )
                    .replace( "$7", act1Desc );
            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            grp1.toJSON( STR_OUT );
            Assert.assertEquals( grp1Desc, STR_OUT.toString() );

            // Activity 2
            String act2Desc = ACT2_DESC_FMT.replace( "$5", Long.toString( act2.getId().get() ) )
                    .replace( "$87", act2.getTypeId().toString() )
                    .replace( "$9", act2.getFile().getName() );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            act2.toJSON( STR_OUT );
            Assert.assertEquals( act2Desc, STR_OUT.toString() );

            // Activity 3
            String act3Desc = ACT2_DESC_FMT.replace( "$5", Long.toString( act3.getId().get() ) )
                    .replace( "$87", act3.getTypeId().toString() )
                    .replace( "$9", act3.getFile().getName() );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            act3.toJSON( STR_OUT );
            Assert.assertEquals( act3Desc, STR_OUT.toString() );

            // Group 2
            String grp2Desc = GRP2_DESC_FMT.replace( "$5", Long.toString( grp2.getId().get() ) )
                    .replace( "$87", grp2.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp2.isRandom() ) )
                    .replace( "$7", act2Desc )
                    .replace( "$TAG", grp2.getTag().toString() )
                    .replace( "$TIME", Integer.toString( grp2.getTimeForEachActivity().getTimeInSeconds() ) );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            grp2.toJSON( STR_OUT );
            Assert.assertEquals( grp2Desc, STR_OUT.toString() );

            // Group 3
            String grp3Desc = GRP3_DESC_FMT.replace( "$5", Long.toString( grp3.getId().get() ) )
                    .replace( "$87", grp3.getTypeId().toString() )
                    .replace( "$6", Boolean.toString( grp3.isRandom() ) )
                    .replace( "$7", act3Desc )
                    .replace( "$TAG", grp3.getTag().toString() );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            grp3.toJSON( STR_OUT );
            Assert.assertEquals( grp3Desc, STR_OUT.toString() );

            expectedJSON = expectedJSON.replace( "$41", grp1Desc )
                    .replace( "$42", grp2Desc )
                    .replace( "$43", grp3Desc );

            STR_OUT.getBuffer().delete( 0, STR_OUT.getBuffer().length() );
            expr2.toJSON( STR_OUT );
            Assert.assertEquals( expectedJSON, STR_OUT.toString() );
        } catch(JSONException exc)
        {
            Assert.fail( exc.getMessage() );
        }

        return;
    }

    private static Result res1;
    private static Result.ActivityChangeEvent event1;
    private static Result.BeatEvent event2;
    private static Result.ActivityChangeEvent event3;
    private static Result.ActivityChangeEvent event4;

    private static Experiment expr1;
    private static Experiment expr2;
    private static ManualGroup grp1;
    private static PictureGroup grp2;
    private static VideoGroup grp3;
    private static MediaGroup.MediaActivity act2;
    private static MediaGroup.MediaActivity act3;
    private static ManualGroup.ManualActivity act1;
}
