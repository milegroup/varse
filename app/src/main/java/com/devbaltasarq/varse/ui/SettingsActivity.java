// VARSE 2019/22 (c) Baltasar for MILEGroup MIT License <baltasarq@uvigo.es>


package com.devbaltasarq.varse.ui;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.DropboxUsrClient;
import com.devbaltasarq.varse.core.Ofm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Settings;
import com.dropbox.core.DbxException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public class SettingsActivity extends AppActivity {
    public final static String LOG_TAG = SettingsActivity.class.getSimpleName();
    private static final int REQ_READ_CONTACTS = 991;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_settings );

        final EditText ED_EMAIL = this.findViewById( R.id.edEmail );
        final ImageButton BT_CLOSE = this.findViewById( R.id.btCloseSettings );
        final ImageButton BT_FORCE_BACKUP = this.findViewById( R.id.btForceBackup );
        final ImageButton BT_RECOVERY = this.findViewById( R.id.btRecovery );
        final ImageButton BT_OBTAIN_EMAIL = this.findViewById( R.id.btObtainEmail );
        final TextView LBL_BACKUP = this.findViewById( R.id.lblBackup );
        final TextView LBL_RECOVERY = this.findViewById( R.id.lblRecovery );
        final CheckBox CHK_LINK = this.findViewById( R.id.chkLink );

        // Listeners
        BT_CLOSE.setOnClickListener( (v) -> SettingsActivity.this.close() );

        View.OnClickListener backupListener = (v) -> SettingsActivity.this.forceBackup();

        LBL_BACKUP.setOnClickListener( backupListener );
        BT_FORCE_BACKUP.setOnClickListener( backupListener );

        View.OnClickListener recoveryListener = (v) -> {
                final SettingsActivity SELF = SettingsActivity.this;
                final AlertDialog.Builder DLG = new AlertDialog.Builder( SELF );

                DLG.setMessage( R.string.msgAreYouSure );
                DLG.setNegativeButton( R.string.lblBack, null );
                DLG.setPositiveButton( R.string.lblRecovery, (dialogInterface, i) -> SELF.recovery() );

                DLG.show();
        };

        LBL_RECOVERY.setOnClickListener( recoveryListener );
        BT_RECOVERY.setOnClickListener( recoveryListener );
        BT_OBTAIN_EMAIL.setOnClickListener( (v) -> this.askUserForEmailAccountAccess() );
        CHK_LINK.setOnCheckedChangeListener( (bt, isChk) -> {
            this.turnCloudOptions( isChk );
        });

        ED_EMAIL.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {}

            @Override
            public void afterTextChanged(Editable editable)
            {
                final String TXT_PROBABLE_EMAIL = ED_EMAIL.getText().toString();
                final Settings SETTINGS = Settings.get();

                if ( Patterns.EMAIL_ADDRESS.matcher( TXT_PROBABLE_EMAIL ).matches() )
                {
                    SETTINGS.setEmail( TXT_PROBABLE_EMAIL );
                } else {
                    SETTINGS.setEmail( "" );
                }
            }
        });

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

        this.hideCloudOptions();
        if ( SETTINGS.isEmailSet() ) {
            final EditText ED_EMAIL = this.findViewById( R.id.edEmail );

            ED_EMAIL.setText( SETTINGS.getEmail() );
            this.showCloudOptions();
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

    private void hideCloudOptions()
    {
        this.turnCloudOptions( false );
    }

    private void showCloudOptions()
    {
        this.turnCloudOptions( true );
    }

    private void turnCloudOptions(boolean on)
    {
        final LinearLayout LY_BACKUP = this.findViewById( R.id.lyBackup );
        final LinearLayout LY_RECOVER = this.findViewById( R.id.lyRecover );
        final LinearLayout LY_ED_EMAIL = this.findViewById( R.id.lyEdEmail );
        final CheckBox CHK_CLOUD = this.findViewById( R.id.chkLink );
        int status = on? View.VISIBLE : View.GONE;

        LY_BACKUP.setVisibility( status );
        LY_RECOVER.setVisibility( status );
        LY_ED_EMAIL.setVisibility( status );
        CHK_CLOUD.setChecked( on );
    }

    /*
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
*/
    /** Recovery from the cloud. **/
    private void recovery()
    {
        final Settings SETTINGS = Settings.get();

        Log.d( LOG_TAG, "Starting recovery..." );

        if ( !SETTINGS.isEmailSet() ) {
            this.showStatus( LOG_TAG, this.getString( R.string.errCloudEmailNotSet ) );
            return;
        }

        if ( !this.cloudOperationFinished ) {
            this.showStatus( LOG_TAG, this.getString( R.string.errAnotherCloudOpRunning ) );
            return;
        }

        final ImageButton BT_RECOVERY = this.findViewById( R.id.btForceBackup );
        final ProgressBar PB_PROGRESS = this.findViewById( R.id.pbProgressRecovery );
        final SettingsActivity SELF = this;
        final Ofm OFM = Ofm.get();
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
                        DBOX_SERVICE.downloadDataFileTo( fileName, OFM );

                        PB_PROGRESS.incrementProgressBy( 1 );
                    }

                    for(Pair<String, String[]> resFileSet: resFiles) {
                        for(String resFile: resFileSet.second) {
                            DBOX_SERVICE.downloadResFileTo( resFileSet.first, resFile, OFM );

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
                        OFM.reset();

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
        final Settings SETTINGS = Settings.get();

        Log.d( LOG_TAG, "Starting backup..." );

        if ( !SETTINGS.isEmailSet() ) {
            this.showStatus( LOG_TAG, this.getString( R.string.errCloudEmailNotSet ) );
            return;
        }

        if ( !this.cloudOperationFinished ) {
            this.showStatus( LOG_TAG, this.getString( R.string.errAnotherCloudOpRunning ) );
            return;
        }

        final ImageButton BT_FORCE_BACKUP = this.findViewById( R.id.btForceBackup );
        final ProgressBar PB_PROGRESS = this.findViewById( R.id.pbProgressCompleteBackup );
        final SettingsActivity SELF = this;
        final Ofm OFM = Ofm.get();
        final String USR_EMAIL = SETTINGS.getEmail();

        BT_FORCE_BACKUP.setEnabled( false );

        this.handlerThread = new HandlerThread( "dropbox_backup" );
        this.handlerThread.start();
        this.handler = new Handler( this.handlerThread.getLooper() );

        this.cloudOperationFinished = false;
        this.handler.post( () -> {
                try {
                    final DropboxUsrClient DBOX_SERVICE = new DropboxUsrClient( this, USR_EMAIL );
                    final Persistent.TypeId[] TYPES = { Persistent.TypeId.Experiment, Persistent.TypeId.Result };

                    // Collect files
                    final ArrayList<File> DATA_FILES = new ArrayList<>();
                    final List<Pair<String, File[]>> RES_FILES = OFM.enumerateResFiles();

                    for(Persistent.TypeId typeId: TYPES) {
                        DATA_FILES.addAll( Arrays.asList( OFM.enumerateFiles( typeId ) ) );
                    }

                    // Find the total number of files
                    int numFiles = DATA_FILES.size();

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

    private void askUserForEmailAccountAccess()
    {
        int permissionResult = ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.GET_ACCOUNTS );

        if ( permissionResult != PackageManager.PERMISSION_GRANTED ) {
            // Should we show an explanation?
            boolean needShowExplanation =
                    ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            android.Manifest.permission.GET_ACCOUNTS );

            if ( needShowExplanation ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{ android.Manifest.permission.GET_ACCOUNTS },
                        REQ_READ_CONTACTS );

                this.obtainEmailFromUser();
            }
        } else {
            this.obtainEmailFromUser();
        }
    }

    private void obtainEmailFromUser()
    {
        final Pattern EMAIL_PATTERN = Patterns.EMAIL_ADDRESS;
        final Account[] ACCOUNTS = AccountManager.get( this ).getAccounts();
        final EditText ED_EMAIL = this.findViewById( R.id.edEmail );
        String email = "";

        for (final Account ACCOUNT : ACCOUNTS) {
            final String POSSIBLE_EMAIL = ACCOUNT.name;

            if ( EMAIL_PATTERN.matcher( POSSIBLE_EMAIL ).matches() ) {
                email = POSSIBLE_EMAIL;
                ED_EMAIL.setText( POSSIBLE_EMAIL );
                this.showStatus( LOG_TAG,
                                 this.getString( R.string.lblEmail ) + ": " + POSSIBLE_EMAIL );
                break;
            }
        }

        if ( email.length() == 0 ) {
            this.showStatus( LOG_TAG, this.getString( R.string.errCloudEmailNotSet ) );
        }

        return;
    }

    private boolean cloudOperationFinished;
    private int scrOrientation;
    private String verificationCodeSent;
    private Handler handler;
    private HandlerThread handlerThread;
}
