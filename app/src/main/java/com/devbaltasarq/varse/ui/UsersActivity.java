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
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.PartialObject;
import com.devbaltasarq.varse.core.User;
import com.devbaltasarq.varse.ui.edituser.ListViewUserEntry;
import com.devbaltasarq.varse.ui.edituser.ListViewUserEntryArrayAdapter;

import java.io.IOException;

public class UsersActivity extends AppActivity {
    private static final String LogTag = UsersActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView( R.layout.activity_users );
        Toolbar toolbar = this.findViewById( R.id.toolbar );
        setSupportActionBar(toolbar);

        final FloatingActionButton fbAdd = this.findViewById( R.id.fbAddUser);
        final ImageButton btCloseUsers = this.findViewById( R.id.btCloseUsers);

        fbAdd.setOnClickListener( (v) -> this.addUser() );
        btCloseUsers.setOnClickListener( (v) -> this.finish() );
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
            final PartialObject[] userList = Orm.get().enumerateUsers();
            final ListView lvUsers = this.findViewById( R.id.lvUsers);
            final ListViewUserEntry[] entries = new ListViewUserEntry[ userList.length ];

            for(int i = 0; i < entries.length; ++i) {
                final PartialObject po = userList[ i ];
                entries[ i ] = new ListViewUserEntry( new User( po.getId(), po.getName() ) );
            }

            ListViewUserEntryArrayAdapter adapter = new ListViewUserEntryArrayAdapter( this,
                                            entries );
            lvUsers.setAdapter( adapter );
        } catch(IOException exc)
        {
            this.showStatus( LogTag, this.getString( R.string.ErrIO ) );
        }
    }

    private void addUser()
    {
        final AlertDialog.Builder db = new AlertDialog.Builder( this );
        final EditText edId = new EditText( this );
        final AppActivity activity = this;

        db.setTitle( R.string.lblAddUser);
        db.setView( edId );

        db.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    Orm.get().store( new User( Id.createFake(), edId.getText().toString() ) );
                    UsersActivity.this.showUsers();
                } catch(IOException exc) {
                    activity.showStatus( LogTag, activity.getString( R.string.ErrIO ) );
                }
            }
        });

        db.create().show();
    }

    public static User selectedUser;
}
