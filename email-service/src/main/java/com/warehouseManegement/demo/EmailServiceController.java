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

    @PostMapping("/sendEmail")
    public String sendEmail(
            @RequestParam String toEmail,
            @RequestParam String subject,
            @RequestParam String body
            )
    {
        System.out.println("Sending email to: " + toEmail);
        System.out.println("Sending email subject: " + subject);
        System.out.println("Sending email body: " + body);
        System.out.println("Trying to send email-POST");
        sendSimpleEmail(toEmail, subject, body);
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
