// VARSE (c) 2019 Baltasar for MILEGroup MIT License <jbgarcia@uvigo.es>

package com.devbaltasarq.varse.core;

import android.app.Activity;

import com.devbaltasarq.varse.R;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.users.FullAccount;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * This class automates access to the Dropbox API.
 */
public class DropboxClient {
    public DropboxClient(Activity owner)
    {
        this.owner = owner;
    }

    public void uploadToDropbox(File f, String userEmail) throws DbxException
    {
        // Create Dropbox client
        DbxRequestConfig config =
                DbxRequestConfig.newBuilder( "com.devbaltasarq.varse" ).build();
        DbxClientV2 client =
                new DbxClientV2( config,
                                 this.owner.getString( R.string.dropbox_token ) );

        // Get current account info
        FullAccount account = client.users().getCurrentAccount();
        System.out.println( account.getName().getDisplayName() );

        // Build target file name
        String targetFileName = "/" + userEmail + "/" + f.getName();

        // Upload file to Dropbox
        try (InputStream in = new FileInputStream( f )) {
            FileMetadata metadata = client.files().uploadBuilder( targetFileName )
                    .uploadAndFinish( in );
        } catch(IOException exc)
        {
            throw new DbxException( exc.getMessage() );
        }
    }

    private Activity owner;
}
