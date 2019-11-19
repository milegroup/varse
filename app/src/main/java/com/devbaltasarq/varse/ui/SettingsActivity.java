package com.devbaltasarq.varse.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.AppInfo;
import com.devbaltasarq.varse.core.DropboxUsrClient;
import com.devbaltasarq.varse.core.MailClient;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Settings;
import com.dropbox.core.DbxException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SettingsActivity extends AppActivity {
    public final static String LOG_TAG = SettingsActivity.class.getSimpleName();

    // TODO: it is needed to backup and recover resource files as well...

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_settings );

        final EditText ED_EMAIL = this.findViewById( R.id.edEmail );
        final ImageButton BT_CLOSE = this.findViewById( R.id.btCloseSettings );
        final ImageButton BT_SEND_EMAIL = this.findViewById( R.id.btSendVerificationEmail );
        final ImageButton BT_VERIFY = this.findViewById( R.id.btVerifyEmail );
        final ImageButton BT_RESET = this.findViewById( R.id.btResetVerification );
        final ImageButton BT_FORCE_BACKUP = this.findViewById( R.id.btForceBackup );
        final ImageButton BT_RECOVERY = this.findViewById( R.id.btRecovery );
        final TextView LBL_BACKUP = this.findViewById( R.id.lblBackup );
        final TextView LBL_RECOVERY = this.findViewById( R.id.lblRecovery );

        // Listeners
        BT_CLOSE.setOnClickListener( (v) -> SettingsActivity.this.close() );

        BT_SEND_EMAIL.setOnClickListener( (v) -> {
                final String EMAIL = ED_EMAIL.getText().toString();

                SettingsActivity.this.sendVerificationEMail( EMAIL );
        });

        BT_VERIFY.setOnClickListener( (v) -> SettingsActivity.this.verifyEmail() );

        BT_RESET.setOnClickListener( (v) -> SettingsActivity.this.resetVerification() );

        View.OnClickListener backupListener = (v) -> SettingsActivity.this.forceBackup();

        LBL_BACKUP.setOnClickListener( backupListener );
        BT_FORCE_BACKUP.setOnClickListener( backupListener );

        View.OnClickListener recoveryListener = (v) -> {
                final SettingsActivity SELF = SettingsActivity.this;
                final AlertDialog.Builder DLG = new AlertDialog.Builder( SELF );

                DLG.setMessage( R.string.msgAreYouSure );
                DLG.setNegativeButton( R.string.lblBack, null );
                DLG.setPositiveButton( R.string.lblRecovery, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        SELF.recovery();
                    }
                });

                DLG.show();
        };

        LBL_RECOVERY.setOnClickListener( recoveryListener );
        BT_RECOVERY.setOnClickListener( recoveryListener );

        // Initialize
        this.cloudOperationFinished = true;

        // Prevent screen rotation
        this.scrOrientation = this.getRequestedOrientation();
        this.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_NOSENSOR );
    }

    @Override
    public void onResume()
    {
        super.onResume();

        final Settings SETTINGS = Settings.get();
        final ImageButton BT_FORCE_BACKUP = this.findViewById( R.id.btForceBackup );

        BT_FORCE_BACKUP.setEnabled( false );

        if ( SETTINGS.isEmailSet() ) {
            final EditText ED_EMAIL = this.findViewById( R.id.edEmail );

            ED_EMAIL.setText( SETTINGS.getEmail() );
            this.showAsEmailVerified();
        }

        return;
    }

    @Override
    public boolean askBeforeLeaving()
    {
        return false;
    }

    @Override
    public void finish()
    {
        if ( !this.cloudOperationFinished) {
            Toast.makeText( this,
                    this.getString( R.string.msgWaitForBackup ),
                    Toast.LENGTH_SHORT ).show();
        } else {
            this.setRequestedOrientation( this.scrOrientation );
            super.finish();
        }

        return;
    }

    private void close()
    {
        this.finish();
    }

    private void showAsEmailVerified()
    {
        final LinearLayout LY_VERIFY_CODE = this.findViewById( R.id.lyVerificationCode );
        final EditText ED_CODE = this.findViewById( R.id.edVerificationCode );
        final EditText ED_EMAIL = this.findViewById( R.id.edEmail );
        final ImageButton BT_FORCE_BACKUP = this.findViewById( R.id.btForceBackup );


        ED_CODE.setText( "" );
        ED_EMAIL.setVisibility( View.VISIBLE );
        ED_EMAIL.setEnabled( false );
        LY_VERIFY_CODE.setVisibility( View.GONE );
        BT_FORCE_BACKUP.setEnabled( true );
    }

    private void sendVerificationEMail(String targetAddress)
    {
        final EditText ED_EMAIL = this.findViewById( R.id.edEmail );
        final EditText ED_CODE = this.findViewById( R.id.edVerificationCode );
        final ImageButton BT_RESET = this.findViewById( R.id.btResetVerification );
        final ImageButton BT_SEND_EMAIL = this.findViewById( R.id.btSendVerificationEmail );
        final ImageButton BT_VERIFY_EMAIL = this.findViewById( R.id.btVerifyEmail );
        final LinearLayout LY_VERIFY_CODE = this.findViewById( R.id.lyVerificationCode );

        // Prepare the ui
        BT_VERIFY_EMAIL.setVisibility( View.VISIBLE );
        BT_RESET.setVisibility( View.VISIBLE );
        LY_VERIFY_CODE.setVisibility( View.VISIBLE );

        BT_SEND_EMAIL.setVisibility( View.GONE );

        ED_EMAIL.setEnabled( false );

        ED_CODE.setEnabled( true );
        ED_CODE.setText( "" );

        // Build the verification code
        this.verificationCodeSent = this.createVerificationCode();

        // Send verification email
        final MailClient MAILER = new MailClient(
                this,
                "mail.gmx.com", 587,
                targetAddress,
                AppInfo.NAME + " " + "Verification email",
                "Verification code:" + this.verificationCodeSent );


        MAILER.sendAuthenticated();
    }

    private void verifyEmail()
    {
        final EditText ED_CODE = this.findViewById( R.id.edVerificationCode );
        final EditText ED_EMAIL = this.findViewById( R.id.edEmail );
        final ImageButton BT_RESTART_VERIFICATION  = this.findViewById( R.id.btResetVerification );
        String enteredCode = ED_CODE.getText().toString();
        String email = ED_EMAIL.getText().toString();

        BT_RESTART_VERIFICATION.setVisibility( View.VISIBLE );
        ED_CODE.setEnabled( false );

        if ( enteredCode.equals( this.verificationCodeSent ) ) {
            this.showAsEmailVerified();
            Toast.makeText( this, "Email verified: " + email, Toast.LENGTH_LONG ).show();
            this.verificationCodeSent = "";
            Settings.get().setEmail( ED_EMAIL.getText().toString() );
        } else {
            Toast.makeText( this, "Verification failed for: " + email, Toast.LENGTH_LONG ).show();

            this.resetVerification();
        }

        return;
    }

    private void resetVerification()
    {
        final LinearLayout LY_VERIFY_CODE = this.findViewById( R.id.lyVerificationCode );
        final ImageButton BT_SEND_EMAIL = this.findViewById( R.id.btSendVerificationEmail );
        final ImageButton BT_VERIFY = this.findViewById( R.id.btVerifyEmail );
        final ImageButton BT_RESET = this.findViewById( R.id.btResetVerification );
        final EditText ED_EMAIL = this.findViewById( R.id.edEmail );
        final EditText ED_CODE = this.findViewById( R.id.edVerificationCode );

        ED_EMAIL.setText( "" );
        ED_CODE.setText( "" );

        BT_SEND_EMAIL.setVisibility( View.VISIBLE );
        BT_VERIFY.setVisibility( View.GONE );
        BT_RESET.setVisibility( View.GONE );
        LY_VERIFY_CODE.setVisibility( View.GONE );
        ED_EMAIL.setEnabled( true );
    }

    /** Create a verification code.
      * @return A verification code, as a string.
      */
    private String createVerificationCode()
    {
        final int RND_NUM_DIGITS = 6;
        final Random RND = new Random();

        // Create an random number
        StringBuilder toret = new StringBuilder( RND_NUM_DIGITS );

        for(int i = 0; i < RND_NUM_DIGITS; ++i) {
            toret.append( Character.toString( (char) ( '0' + RND.nextInt(10 ) ) ) );
        }

        return toret.toString();
    }

    /** Recovery from the cloud. **/
    private void recovery()
    {
        Log.d( LOG_TAG, "Starting recovery..." );

        if ( !this.cloudOperationFinished ) {
            Log.d( LOG_TAG, "Recovery: previous operation unfinished, bouncing out." );
            return;
        }

        final ImageButton BT_RECOVERY = this.findViewById( R.id.btForceBackup );
        final ProgressBar PB_PROGRESS = this.findViewById( R.id.pbProgressRecovery );
        final SettingsActivity SELF = this;
        final Orm ORM = Orm.get();
        final String USER_EMAIL = Settings.get().getEmail();

        BT_RECOVERY.setEnabled( false );

        this.cloudOperationFinished = false;
        this.handlerThread = new HandlerThread( "dropbox_backup" );
        this.handlerThread.start();
        this.handler = new Handler( this.handlerThread.getLooper() );

        this.handler.post( () -> {
                try {
                    final DropboxUsrClient DBOX_SERVICE = new DropboxUsrClient( this, USER_EMAIL );

                    // Collect files
                    String[] dataFiles = DBOX_SERVICE.listUsrFiles();
                    List<Pair<String, String[]>> resFiles = DBOX_SERVICE.listUsrResFiles();
                    int numFiles = dataFiles.length;

                    // Calculate the number of files
                    for(Pair<String, String[]> resFile: resFiles) {
                        numFiles += resFile.second.length;
                    }

                    PB_PROGRESS.setMax( numFiles );
                    PB_PROGRESS.setProgress( 0 );

                    SELF.runOnUiThread( () ->
                        PB_PROGRESS.setVisibility( View.VISIBLE )
                    );

                    // Download everything
                    for(String fileName: dataFiles) {
                        DBOX_SERVICE.downloadDataFileTo( fileName, ORM );

                        PB_PROGRESS.incrementProgressBy( 1 );
                    }

                    for(Pair<String, String[]> resFileSet: resFiles) {
                        for(String resFile: resFileSet.second) {
                            DBOX_SERVICE.downloadResFileTo( resFileSet.first, resFile, ORM );

                            PB_PROGRESS.incrementProgressBy( 1 );
                        }
                    }

                    SELF.runOnUiThread(
                            () -> SELF.showStatus( LOG_TAG, SELF.getString( R.string.msgFinishedRecovery ) ) );
                } catch (DbxException exc)
                {
                    SELF.runOnUiThread(
                            () -> SELF.showStatus( LOG_TAG, SELF.getString( R.string.errIO ) ) );

                } finally {
                    SELF.runOnUiThread( () -> {
                        PB_PROGRESS.setVisibility( View.GONE );
                        BT_RECOVERY.setEnabled( true );
                        ORM.reset();

                        SELF.handler.removeCallbacksAndMessages( null );
                        SELF.handlerThread.quit();
                        SELF.cloudOperationFinished = true;
                    });
                }
        });

        return;
    }

    /** Backup all now. */
    private void forceBackup()
    {
        Log.d( LOG_TAG, "Starting backup..." );

        if ( !this.cloudOperationFinished ) {
            Log.d( LOG_TAG, "Backup: previous operation unfinished, bouncing out." );
            return;
        }

        final ImageButton BT_FORCE_BACKUP = this.findViewById( R.id.btForceBackup );
        final ProgressBar PB_PROGRESS = this.findViewById( R.id.pbProgressCompleteBackup );
        final SettingsActivity SELF = this;
        final Orm ORM = Orm.get();
        final String USR_EMAIL = Settings.get().getEmail();

        BT_FORCE_BACKUP.setEnabled( false );

        this.handlerThread = new HandlerThread( "dropbox_backup" );
        this.handlerThread.start();
        this.handler = new Handler( this.handlerThread.getLooper() );

        this.cloudOperationFinished = false;
        this.handler.post( () -> {
                try {
                    final DropboxUsrClient DBOX_SERVICE = new DropboxUsrClient( this, USR_EMAIL );

                    // Collect files
                    final ArrayList<File> DATA_FILES = new ArrayList<>();
                    final List<Pair<String, File[]>> RES_FILES = ORM.enumerateResFiles();
                    int numFiles = 0;

                    for(Persistent.TypeId typeId: Persistent.TypeId.values()) {
                        DATA_FILES.addAll( Arrays.asList( ORM.enumerateFiles( typeId ) ) );
                    }

                    // Find the total number of files
                    numFiles = DATA_FILES.size();

                    for(Pair<String, File[]> subDir: RES_FILES) {
                        numFiles += subDir.second.length;
                    }

                    PB_PROGRESS.setMax( numFiles );
                    PB_PROGRESS.setProgress( 0 );

                    SELF.runOnUiThread( () -> PB_PROGRESS.setVisibility( View.VISIBLE ) );

                    // Upload data files
                    for(File f: DATA_FILES) {
                        DBOX_SERVICE.uploadDataFile( f );
                        PB_PROGRESS.incrementProgressBy( 1 );
                    }

                    // Upload resource files
                    for(Pair<String, File[]> pair: RES_FILES) {
                        for(File f: pair.second) {
                            DBOX_SERVICE.uploadResFile( pair.first, f );
                            PB_PROGRESS.incrementProgressBy( 1 );
                        }
                    }

                    DATA_FILES.clear();

                    SELF.runOnUiThread( () -> SELF.showStatus( LOG_TAG, SELF.getString( R.string.msgFinishedBackup ) ) );
                } catch (DbxException exc)
                {
                    SELF.runOnUiThread( () -> SELF.showStatus( LOG_TAG, SELF.getString( R.string.errIO ) ) );
                } finally {
                    SELF.runOnUiThread( () -> {
                            PB_PROGRESS.setVisibility( View.GONE );
                            BT_FORCE_BACKUP.setEnabled( true );

                            SELF.handler.removeCallbacksAndMessages( null );
                            SELF.handlerThread.quit();
                            SELF.cloudOperationFinished = true;
                    });
                }
        });

        return;
    }

    private boolean cloudOperationFinished;
    private int scrOrientation;
    private String verificationCodeSent;
    private Handler handler;
    private HandlerThread handlerThread;
}
