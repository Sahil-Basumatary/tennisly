package dev.sahilbasumatary.notificationservice.email;

public record EmailMessage(String to, String subject, String htmlBody, String textBody) {}
