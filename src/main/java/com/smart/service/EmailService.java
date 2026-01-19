package com.smart.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	@Value("${sendgrid.api.key}")
	private String apiKey;

	@Value("${sendgrid.mail.from}")
	private String fromEmail;

    public boolean sendEmail(String subject, String message, String to) {

        try {
            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", message);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            System.out.println("SENDGRID KEY PRESENT: " + (apiKey != null));
            System.out.println("FROM EMAIL: " + fromEmail);

            return response.getStatusCode() >= 200 && response.getStatusCode() < 300;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
