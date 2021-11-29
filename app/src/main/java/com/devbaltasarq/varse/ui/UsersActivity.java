package com.devbaltasarq.varse.ui;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.Id;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.ofmcache.PartialObject;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.ui.adapters.ListViewUserArrayAdapter;

import java.io.IOException;


public class UsersActivity extends AppActivity {
    private static final String LOG_TAG = UsersActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_users );
        Toolbar toolbar = this.findViewById( R.id.toolbar );
        setSupportActionBar(toolbar);

        final FloatingActionButton FB_ADD = this.findViewById( R.id.fbAddUser );
        final ImageButton BT_CLOSE_USERS = this.findViewById( R.id.btCloseUsers);

        FB_ADD.setOnClickListener( (v) -> this.addUser() );
        BT_CLOSE_USERS.setOnClickListener( (v) -> this.finish() );
        this.setTitle( "" );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        this.showUsers();
    }

    private void showUsers()
    {
        try {
            final PartialObject[] USR_LIST = Ofm.get().enumerateUsers();
            final ListView LV_USRS = this.findViewById( R.id.lvUsers);
            final User[] ENTRIES = new User[ USR_LIST.length ];

            // Create a fake list of users
            for(int i = 0; i < ENTRIES.length; ++i) {
                final PartialObject PO = USR_LIST[ i ];

                ENTRIES[ i ] = new User( PO.getId(), PO.getName() );
            }

            ListViewUserArrayAdapter adapter = new ListViewUserArrayAdapter( this,
                                                        ENTRIES );
            LV_USRS.setAdapter( adapter );
        } catch(IOException exc)
        {
            this.showStatus(LOG_TAG, this.getString( R.string.errIO) );
        }
    }

    private void addUser()
    {
        final AlertDialog.Builder db = new AlertDialog.Builder( this );
        final EditText edId = new EditText( this );
        final AppActivity activity = this;

        db.setTitle( R.string.lblAddUser);
        db.setView( edId );

        db.setPositiveButton(android.R.string.ok, (DialogInterface dialogInterface, int i) -> {
            try {
                Ofm.get().store( new User( Id.createFake(), edId.getText().toString() ) );
                UsersActivity.this.showUsers();
            } catch(IOException exc) {
                activity.showStatus(LOG_TAG, activity.getString( R.string.errIO) );
            }
        });

        db.create().show();
    }

    public static User selectedUser;
}
