package com.warehouseManegement.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/email")
public class EmailServiceController {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailSender;

    @Value("${spring.mail.password}")
    private String mailPassword;

    public void sendSimpleEmail(String toEmail, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailSender);
        message.setTo(toEmail);
        message.setText(body);
        message.setSubject(subject);
        mailSender.send(message);
    }

    public static class EmailRequest {
        public String toEmail;
        public String subject;
        public String body;
    }

    @PostMapping("/sendEmail")
    public String sendEmail(@RequestBody EmailRequest emailRequest)
    {
        System.out.println("Sending email to: " + emailRequest.toEmail);
        System.out.println("Sending email subject: " + emailRequest.subject);
        System.out.println("Sending email body: " + emailRequest.body);
        System.out.println("Trying to send email-POST");
        sendSimpleEmail(emailRequest.toEmail, emailRequest.subject, emailRequest.body);
        System.out.println("It is invoked after sending email!");
        return "emailSent";
    }




    @GetMapping("/sendEmail")
    public String sendEmailGet(
    )
    {
        System.out.println("Trying to send email-GET");
        System.out.println("Mail username: "+emailSender);
        System.out.println("Mail password: "+mailPassword);

        return "emailSent";
    }

}
