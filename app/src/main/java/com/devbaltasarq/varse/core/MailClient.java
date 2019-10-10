// VARSE (c) 2019 Baltasar for MILEGroup MIT License <jbgarcia@uvigo.es>

package com.devbaltasarq.varse.core;

import android.app.Activity;
import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.Toast;

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

    public MailClient(Activity owner,
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
        this.ccList = null;
        this.bccList = null;
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
            final Activity OWNER = MailClient.this.owner;

            try {
                this.doSend();

                OWNER.runOnUiThread( () -> {
                    Toast.makeText( OWNER, "Mail sent.", Toast.LENGTH_LONG ).show();
                });
            } catch(MessagingException exc) {
                Log.e( LOG_TAG, exc.getMessage() );

                OWNER.runOnUiThread( () -> {
                    Toast.makeText( OWNER, "Failed sending email.", Toast.LENGTH_LONG ).show();
                });
            }

            MailClient.this.handler.removeCallbacksAndMessages( null );
            MailClient.this.handlerThread.quit();
        });
    }

    /** Send an e-mail
      * @throws MessagingException
      * @throws AddressException
      */
    private void doSend() throws AddressException, MessagingException
    {
        final Properties PROPS = new Properties();
        Session session;

        // set the host smtp address
        PROPS.put( "mail.transport.protocol", "smtp" );
        PROPS.put( "mail.smtp.host", this.smtpServer );
        PROPS.put( "mail.user", from );
        PROPS.put( "mail.smtp.user", from );
        PROPS.put( "mail.smtp.password", this.owner.getString( R.string.mail_auth_pswd ) );

        if ( this.authenticationRequired ) {
            PROPS.put( "mail.smtp.starttls.enable", "true" );
            PROPS.put( "mail.smtp.auth", "true" );
            PROPS.put( "mail.smtp.port", Integer.toString( this.port ) );

            session = Session.getDefaultInstance( PROPS, pswdAuth );
        } else {
            session = Session.getDefaultInstance( PROPS, null );
        }

        // get the default session
        if ( Debug.isDebuggerConnected() ) {
            session.setDebug( true );
        }

        // create message
        final Message MSG = new javax.mail.internet.MimeMessage(session);

        // set from and to address
        try {
            MSG.setFrom(new InternetAddress(from, from));
            MSG.setReplyTo(new InternetAddress[]{new InternetAddress(from,from)});
        } catch (Exception e) {
            MSG.setFrom(new InternetAddress(from));
            MSG.setReplyTo(new InternetAddress[]{new InternetAddress(from)});
        }

        // set send date
        MSG.setSentDate(Calendar.getInstance().getTime());

        // parse the recipients TO address
        java.util.StringTokenizer st = new java.util.StringTokenizer(toList, ",");
        int numberOfRecipients = st.countTokens();

        javax.mail.internet.InternetAddress[] addressTo = new javax.mail.internet.InternetAddress[numberOfRecipients];

        int i = 0;
        while (st.hasMoreTokens()) {
            addressTo[i++] = new javax.mail.internet.InternetAddress(st
                    .nextToken());
        }
        MSG.setRecipients(javax.mail.Message.RecipientType.TO, addressTo);

        // parse the recipients CC address
        if (ccList != null && !"".equals(ccList)) {
            st = new java.util.StringTokenizer(ccList, ",");
            int numberOfCCRecipients = st.countTokens();

            javax.mail.internet.InternetAddress[] addressCC = new javax.mail.internet.InternetAddress[numberOfCCRecipients];

            i = 0;
            while (st.hasMoreTokens()) {
                addressCC[i++] = new javax.mail.internet.InternetAddress(st
                        .nextToken());
            }

            MSG.setRecipients(javax.mail.Message.RecipientType.CC, addressCC);
        }

        // parse the recipients BCC address
        if (bccList != null && !"".equals(bccList)) {
            st = new java.util.StringTokenizer(bccList, ",");
            int numberOfBCCRecipients = st.countTokens();

            javax.mail.internet.InternetAddress[] addressBCC = new javax.mail.internet.InternetAddress[numberOfBCCRecipients];

            i = 0;
            while (st.hasMoreTokens()) {
                addressBCC[i++] = new javax.mail.internet.InternetAddress(st
                        .nextToken());
            }

            MSG.setRecipients(javax.mail.Message.RecipientType.BCC, addressBCC);
        }

        // Set header
        MSG.addHeader("X-Mailer",
                    AppInfo.NAME + MailClient.class.getSimpleName() );

        // Setting the subject and content type
        MSG.setSubject(subject);

        Multipart mp = new MimeMultipart( "related" );

        // Set body message
        MimeBodyPart bodyMsg = new MimeBodyPart();
        bodyMsg.setText( this.txtBody, "utf-8" );
        mp.addBodyPart( bodyMsg );
        MSG.setContent( mp );

        // Send it
        javax.mail.Transport.send( MSG );
    }

    public String getToList() {
        return toList;
    }

    public void setToList(String toList) {
        this.toList = toList;
    }

    public String getCcList() {
        return ccList;
    }

    public void setCcList(String ccList) {
        this.ccList = ccList;
    }

    public String getBccList() {
        return bccList;
    }

    public void setBccList(String bccList) {
        this.bccList = bccList;
    }

    public String getSubject() {
        return subject;
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
        return authenticationRequired;
    }

    public void setAuthenticationRequired(boolean authenticationRequired) {
        this.authenticationRequired = authenticationRequired;
    }

    private String smtpServer;
    private String serverDomain;
    private int port;
    private String toList;
    private String ccList;
    private String bccList;
    private String subject;
    private String from;
    private String txtBody;
    private Activity owner;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean authenticationRequired = false;
    private static Authenticator pswdAuth;
}
