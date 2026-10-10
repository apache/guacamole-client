/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.guacamole.auth.jdbc.reset;

import com.google.inject.Inject;
import java.util.Date;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.jdbc.JDBCEnvironment;
import org.apache.guacamole.auth.jdbc.reset.PasswordResetProperties.SmtpTls;

/**
 * Sends plain-text mail through the SMTP server configured in
 * guacamole.properties.
 */
public class PasswordResetMailer {

    /**
     * SMTP connect, read and write timeout, in milliseconds.
     */
    private static final String TIMEOUT = "10000";

    /**
     * The environment from which SMTP configuration is read.
     */
    @Inject
    private JDBCEnvironment environment;

    /**
     * Sends a plain-text message.
     *
     * @param to
     *     The recipient address.
     *
     * @param subject
     *     The subject line.
     *
     * @param body
     *     The message body.
     *
     * @throws GuacamoleException
     *     If SMTP is not configured or the message cannot be sent.
     */
    public void send(String to, String subject, String body)
            throws GuacamoleException {

        SmtpTls tls = environment.getProperty(PasswordResetProperties.SMTP_TLS, SmtpTls.STARTTLS);
        String username = environment.getProperty(PasswordResetProperties.SMTP_USERNAME);

        Properties props = new Properties();
        props.setProperty("mail.smtp.host", environment.getRequiredProperty(PasswordResetProperties.SMTP_HOSTNAME));
        props.setProperty("mail.smtp.port", String.valueOf(environment.getProperty(
                PasswordResetProperties.SMTP_PORT, tls == SmtpTls.TLS ? 465 : 587)));
        props.setProperty("mail.smtp.connectiontimeout", TIMEOUT);
        props.setProperty("mail.smtp.timeout", TIMEOUT);
        props.setProperty("mail.smtp.writetimeout", TIMEOUT);
        props.setProperty("mail.smtp.auth", String.valueOf(username != null));

        if (tls == SmtpTls.STARTTLS) {
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.starttls.required", "true");
            props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        }
        else if (tls == SmtpTls.TLS) {
            props.setProperty("mail.smtp.ssl.enable", "true");
            props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
        }

        // JavaMail locates its MIME handlers through the context class loader,
        // which inside Tomcat is the webapp's, not this extension's.
        Thread thread = Thread.currentThread();
        ClassLoader original = thread.getContextClassLoader();
        thread.setContextClassLoader(PasswordResetMailer.class.getClassLoader());
        try {

            MimeMessage message = new MimeMessage(Session.getInstance(props));
            message.setFrom(new InternetAddress(environment.getRequiredProperty(PasswordResetProperties.SMTP_FROM)));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(to, true));
            message.setSubject(subject, "UTF-8");
            message.setText(body, "UTF-8");
            message.setSentDate(new Date());

            if (username != null)
                Transport.send(message, username,
                        environment.getProperty(PasswordResetProperties.SMTP_PASSWORD));
            else
                Transport.send(message);

        }
        catch (MessagingException e) {
            throw new GuacamoleServerException("Unable to send mail.", e);
        }
        finally {
            thread.setContextClassLoader(original);
        }

    }

}
