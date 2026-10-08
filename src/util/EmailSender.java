package util;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.sql.*;
import java.util.*;

public class EmailSender {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";

    private static final String SENDER_EMAIL = "attendance.system.project.vit@gmail.com";
    private static final String SENDER_PASSWORD = "yenwaxckqbyfbgmo"; 

    public static List<String> getAllStudentEmails() {
        List<String> emails = new ArrayList<>();

        String sql = "SELECT email FROM students WHERE email IS NOT NULL AND email != ''";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                emails.add(rs.getString("email"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return emails;
    }

    public static boolean sendURLToAll(String url) {

        List<String> recipients = getAllStudentEmails();

        if (recipients.isEmpty()) {
            System.out.println("❌ No recipients found!");
            return false;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props,
                new jakarta.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

        try {
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(SENDER_EMAIL));

            // BCC all recipients
            Address[] bccList = new Address[recipients.size()];
            for (int i = 0; i < recipients.size(); i++) {
                bccList[i] = new InternetAddress(recipients.get(i));
            }

            msg.setRecipients(Message.RecipientType.BCC, bccList);

            msg.setSubject("Attendance Session Link");
            msg.setText("Use the link below:\n\n" + url);

            Transport.send(msg);

            System.out.println("✅ Email sent to all students successfully!");
            return true;

        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return false;
    }
}
