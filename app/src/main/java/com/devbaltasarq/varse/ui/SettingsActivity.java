package com.devbaltasarq.varse.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.devbaltasarq.varse.R;
import com.devbaltasarq.varse.core.AppInfo;
import com.devbaltasarq.varse.core.DropboxClient;
import com.devbaltasarq.varse.core.MailClient;
import com.devbaltasarq.varse.core.Orm;
import com.devbaltasarq.varse.core.Persistent;
import com.devbaltasarq.varse.core.Settings;
import com.dropbox.core.DbxException;

import java.io.File;
import java.io.IOException;
import java.util.Random;

public class SettingsActivity extends AppActivity {
    public final static String LOG_TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        this.setContentView( R.layout.activity_settings );

        final EditText ED_EMAIL = this.findViewById( R.id.edEmail );
        final ImageButton BT_CLOSE = this.findViewById( R.id.btCloseSettings );
        final ImageButton BT_SEND_EMAIL = this.findViewById( R.id.btSendVerificationEmail );
        final ImageButton BT_VERIFY = this.findViewById( R.id.btVerifyEmail );
        final ImageButton BT_RESET = this.findViewById( R.id.btResetVerification );
        final ImageButton BT_FORCE_BACKUP = this.findViewById( R.id.btForceBackup );

        // Listeners
        BT_CLOSE.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity.this.close();
            }
        });

        BT_SEND_EMAIL.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String EMAIL = ED_EMAIL.getText().toString();

                SettingsActivity.this.sendVerificationEMail( EMAIL );
            }
        });

        BT_VERIFY.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity.this.verifyEmail();
            }
        });

        BT_RESET.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity.this.resetVerification();
            }
        });

        BT_FORCE_BACKUP.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SettingsActivity.this.forceBackup();
            }
        });
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
        String toret = "";

        for(int i = 0; i < RND_NUM_DIGITS; ++i) {
            toret += Character.toString( (char) ( '0' + RND.nextInt(10 ) ) );
        }

        return toret;
    }

    /** Backup all now. */
    private void forceBackup()
    {
        final ImageButton BT_FORCE_BACKUP = this.findViewById( R.id.btForceBackup );
        final SettingsActivity SELF = this;
        final Orm ORM = Orm.get();
        final DropboxClient DBOX_SERVICE = new DropboxClient( this );

        BT_FORCE_BACKUP.setEnabled( false );

        this.handlerThread = new HandlerThread( "dropbox_backup" );
        this.handlerThread.start();
        this.handler = new Handler( this.handlerThread.getLooper() );

        this.handler.post( new Runnable() {
            @Override
            public void run() {
                try {
                    // Upload everything
                    for(Persistent.TypeId typeId: Persistent.TypeId.values()) {
                        for(File f: ORM.enumerateFiles( typeId ) ) {
                            DBOX_SERVICE.uploadToDropbox( f, Settings.get().getEmail() );
                        }
                    }

                    SELF.runOnUiThread( new Runnable() {
                        @Override
                        public void run() {
                            SELF.showStatus( LOG_TAG, SELF.getString( R.string.msgFinishedBackup ) );
                        }
                    });
                } catch (IOException | DbxException exc)
                {
                    SELF.runOnUiThread( new Runnable() {
                        @Override
                        public void run() {
                            SELF.showStatus( LOG_TAG, SELF.getString( R.string.errIO ) );
                        }
                    });

                } finally {
                    SELF.runOnUiThread( new Runnable() {
                        @Override
                        public void run() {
                            BT_FORCE_BACKUP.setEnabled( true );

                            SettingsActivity.this.handler.removeCallbacksAndMessages( null );
                            SettingsActivity.this.handlerThread.quit();
                        }
                    });
                }
            }
        });

        return;
    }

    private String verificationCodeSent;
    private Handler handler;
    private HandlerThread handlerThread;
}
