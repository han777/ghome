package com.apartment.service;

import jakarta.annotation.PostConstruct;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Value("${exchange.ews-url}")
    private String ewsUrl;

    @Value("${exchange.username}")
    private String username;

    @Value("${exchange.password}")
    private String password;

    private ExchangeService exchangeService;

    @PostConstruct
    public void init() {
        exchangeService = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
        exchangeService.setCredentials(new WebCredentials(username, password));
        try {
            exchangeService.setUrl(new URI(ewsUrl));
        } catch (Exception e) {
            log.error("Invalid EWS URL: {}", ewsUrl, e);
            throw new RuntimeException("Failed to initialize ExchangeService", e);
        }
        log.info("ExchangeService initialized: url={}, user={}", ewsUrl, username);
    }

    public boolean sendEmail(String to, String subject, String content) {
        try {
            EmailMessage msg = new EmailMessage(exchangeService);
            msg.setSubject(subject);
            msg.setBody(new MessageBody(content));
            msg.getToRecipients().add(to);
            msg.send();
            log.info("Email sent successfully to {} via EWS", to);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email to " + to + ": " + e.getMessage(), e);
        }
    }
}
