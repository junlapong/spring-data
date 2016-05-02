package com.nixmash.springdata.mail.service;

import com.nixmash.springdata.mail.common.MailSettings;
import com.nixmash.springdata.mail.components.MailSender;
import com.nixmash.springdata.mail.dto.MailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import org.springframework.ui.velocity.VelocityEngineUtils;
import org.apache.velocity.app.VelocityEngine;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Hashtable;
import java.util.Map;

@Service("mailService")
public class MailServiceImpl implements MailService{

    private static final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);
    private static final String CONTACT_EMAIL_SUBJECT = "mail.contact.subject";

    final private MailSender mailSender;
    final private MailSettings mailSettings;
    final private VelocityEngine velocityEngine;

    @Autowired
    Environment environment;

    @Value("${mail.contact.body.type}")
    private MailDTO.Type mailType;


    @Autowired
    public MailServiceImpl(MailSender mailSender, MailSettings mailSettings, VelocityEngine velocityEngine) {
        this.mailSender = mailSender;
        this.mailSettings = mailSettings;
        this.velocityEngine = velocityEngine;
    }

    @Override
    public void sendContactMail(MailDTO mailDTO) {
        try {
            mailSender.send(new MimeMessagePreparator() {
                public void prepare(MimeMessage mimeMessage)
                        throws MessagingException {
                    MimeMessageHelper message = new MimeMessageHelper(mimeMessage, false, "UTF-8");
                    message.setFrom(mailDTO.getFrom());
                    message.addTo(mailSettings.getContactTo());

                    if (mailSettings.getSendContactCC()) {
                        message.addCc(mailSettings.getContactCC());
                    }

                    String subject = environment.getProperty(CONTACT_EMAIL_SUBJECT);
                    message.setSubject(MessageFormat.format(subject, mailDTO.getFromName()));

                    String body = mailDTO.getBody();
                    switch (mailType) {
                        case HTML:
                            StringWriter w = new StringWriter();
                            Map<String,Object> model = new Hashtable<String,Object>();
                            model.put("message", mailDTO);
                            try
                            {
                                VelocityEngineUtils.mergeTemplate(velocityEngine, "contact.vm", "UTF-8", model, w );
                                message.setText(w.toString(), true);
                            }
                            catch (Exception e )
                            {
                                System.out.println("Problem merging template : " + e );
                            }

                            break;
                        default:
                            message.setText(body);
                            break;
                    }
                }
            });
        } catch (MailSendException e) {
            logger.error("Contact Email Exception: " + e.getMessage());
        }

    }

}