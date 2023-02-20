// VARSE 2019/23 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.ui.performexperiment;


import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;


/** Represents a chronometer. */
public class Chronometer {
    /** Interface for listeners. */
    public interface Listener<T> {
        void handle(T sender);
    }


    /** Creates a new chronometer with an event handler. */
    public Chronometer(Listener<Chronometer> eventHandler)
    {
        this.handler = new Handler( Looper.getMainLooper() );
        this.eventHandler = eventHandler;
        this.startTime = 0;
        this.stopped = false;
    }

    /** @return the starting time. */
    public long getBase()
    {
        return this.startTime;
    }

    /** @return the elapsed duration, in milliseconds. */
    public long getMillis()
    {
        return SystemClock.elapsedRealtime() - this.startTime;
    }

    /** Resets the current elapsed time with the current real time. */
    public void reset()
    {
        this.reset( SystemClock.elapsedRealtime() );
    }

    /** Resets the current elapsed time with the given time. */
    public void reset(long time)
    {
        this.startTime = time;
    }

    /** Starts the chronometer */
    public void start()
    {
        this.stopped = false;

        this.sendHR = () -> {
            if ( ! this.stopped ) {
                this.eventHandler.handle( this );
                this.handler.postDelayed( this.sendHR,1000);
            }
        };

        this.handler.post( this.sendHR );
    }

    /** Eliminates the daemon so the crono is stopped. */
    public void stop()
    {
        this.stopped = true;
        this.handler.removeCallbacks( this.sendHR );
        this.handler.removeCallbacksAndMessages( null );
    }

    private boolean stopped;
    private long startTime;
    private Runnable sendHR;
    private final Handler handler;
    private final Listener<Chronometer> eventHandler;
}
