// VARSE (c) 2019 Baltasar for MILEGroup MIT License <jbgarcia@uvigo.es>

package com.devbaltasarq.varse.core;

import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.devbaltasarq.varse.R;

import java.util.Calendar;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;


public class MailClient {
    private static final String LOG_TAG = MailClient.class.getSimpleName();

    /**
     * SimpleAuthenticator is used to do simple authentication when the SMTP
     * server requires it.
     */
    final private class SMTPAuthenticator extends javax.mail.Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication()
        {
            return new PasswordAuthentication(
                    MailClient.this.from,
                    MailClient.this.owner.getString( R.string.mail_auth_pswd ) );
        }
    }

    public MailClient(AppCompatActivity owner,
                      String smtpServer, int port,
                      String toList, String subject, String txtBody)
    {
        // Set attributes
        this.smtpServer = smtpServer;
        this.port = port;
        this.txtBody = txtBody;
        this.subject = subject;
        this.from = AppInfo.APP_EMAIL;
        this.toList = toList;
        this.authenticationRequired = true;

        this.owner = owner;

        // Recover email usr & pswd, if needed
        if ( pswdAuth == null ) {
            pswdAuth = new SMTPAuthenticator();
        }
    }

    public void sendAuthenticated()
    {
        this.authenticationRequired = true;
        this.send();
    }

    public void send()
    {
        // Create handler and assure it is in a different thread
        this.handlerThread = new HandlerThread( "mail_sender_thread" );
        this.handlerThread.start();
        this.handler = new Handler( this.handlerThread.getLooper() );

        // Call the doSend() method in a different thread.
        this.handler.post( () -> {
            final AppCompatActivity OWNER = MailClient.this.owner;

            try {
                this.doSend();

                OWNER.runOnUiThread( () ->
                    Toast.makeText( OWNER, "Mail sent.", Toast.LENGTH_LONG ).show()
                );
            } catch(MessagingException exc) {
                Log.e( LOG_TAG, exc.getMessage() );

                OWNER.runOnUiThread( () ->
                    Toast.makeText( OWNER, "Failed sending email.", Toast.LENGTH_LONG ).show()
                );
            }

            MailClient.this.handler.removeCallbacksAndMessages( null );
            MailClient.this.handlerThread.quit();
        });
    }

    /** Send an e-mail
      * @throws MessagingException if message's body is incorrect
      * @throws AddressException if any address is incorrect
      */
    private void doSend() throws AddressException, MessagingException
    {
        final Properties PROPS = new Properties();
        Session session;

        // Set the host smtp address
        PROPS.put( "mail.transport.protocol", "smtp" );
        PROPS.put( "mail.smtp.host", this.smtpServer );
        PROPS.put( "mail.user", this.from );
        PROPS.put( "mail.smtp.user", this.from );
        PROPS.put( "mail.smtp.password", this.owner.getString( R.string.mail_auth_pswd ) );

        if ( this.authenticationRequired ) {
            PROPS.put( "mail.smtp.starttls.enable", "true" );
            PROPS.put( "mail.smtp.auth", "true" );
            PROPS.put( "mail.smtp.port", Integer.toString( this.port ) );

            session = Session.getDefaultInstance( PROPS, pswdAuth );
        } else {
            session = Session.getDefaultInstance( PROPS, null );
        }

        // Get the default session
        if ( Debug.isDebuggerConnected() ) {
            session.setDebug( true );
        }

        // Create message
        final Message MSG = new javax.mail.internet.MimeMessage( session );

        // Set from and to address
        try {
            MSG.setFrom( new InternetAddress( this.from, this.from ) );
            MSG.setReplyTo( new InternetAddress[]{ new InternetAddress( this.from, this.from ) } );
        } catch (Exception e) {
            MSG.setFrom( new InternetAddress( this.from ) );
            MSG.setReplyTo( new InternetAddress[]{ new InternetAddress( this.from ) } );
        }

        // Set send date
        MSG.setSentDate( Calendar.getInstance().getTime() );

        // parse the recipients TO address
        String[] toAddresses = this.toList.split( "," );
        int numberOfRecipients = toAddresses.length;
        javax.mail.internet.InternetAddress[] addressTo =
                new javax.mail.internet.InternetAddress[ numberOfRecipients ];

        for(int i = 0; i < numberOfRecipients; ++i) {
            addressTo[ i ] = new javax.mail.internet.InternetAddress( toAddresses[ i ] );
        }

        MSG.setRecipients( javax.mail.Message.RecipientType.TO, addressTo );

        // Set header
        MSG.addHeader("X-Mailer",
                    AppInfo.NAME + MailClient.class.getSimpleName() );

        // Set body message
        Multipart mp = new MimeMultipart( "related" );

        MimeBodyPart bodyMsg = new MimeBodyPart();
        bodyMsg.setText( this.txtBody, "utf-8" );
        mp.addBodyPart( bodyMsg );

        MimeBodyPart bodyHtml = new MimeBodyPart();
        bodyHtml.setContent( "<b>" + this.txtBody + "</b>", "text/html; charset=utf-8" );
        mp.addBodyPart( bodyHtml );

        // Setting the subject and content type
        MSG.setSubject( this.subject );
        MSG.setContent( mp );

        // Send it
        MSG.saveChanges();
        javax.mail.Transport.send( MSG );
    }

    public String getToList() {
        return this.toList;
    }

    public void setToList(String toList) {
        this.toList = toList;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTxtBody(String body) {
        this.txtBody = body;
    }

    public boolean isAuthenticationRequired() {
        return this.authenticationRequired;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    private String smtpServer;
    private int port;
    private String toList;
    private String subject;
    private String from;
    private String txtBody;
    private AppCompatActivity owner;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean authenticationRequired = false;
    private static Authenticator pswdAuth;
}
