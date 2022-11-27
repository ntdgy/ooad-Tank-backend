package tank.ooad.fitgub.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
public class MailService {
    private static final String SMTP_HOST_NAME = "smtp.office365.com";
    private static final int SMTP_HOST_PORT = 587;
    private static final String SMTP_AUTH_USER = "ooad@dgy.ac.cn";
    private static final String SMTP_AUTH_PWD = "fc447471-1cb7-4914-b15f-daa7fd609255";

    public String sendMail(String receiver, String subject, String content) {
        String url = "http://127.0.0.1:8090";
        String uuid = "7a095c01-0930-403c-85bb-41bbe10eb89c";

        Map<String,String> map = Map.of("receiver", receiver, "subject", subject,
                "content", content, "uuid", uuid);

        RestTemplate restTemplate = new RestTemplate();
        try{
            String result = restTemplate.postForObject(url, map, String.class);
            return result;}
        catch (Exception ignored){}
//        try {
//            Properties props = new Properties();
//            props.put("mail.smtp.host", SMTP_HOST_NAME);
//            props.put("mail.smtp.auth", "true");
//            props.put("mail.smtp.starttls.enable", "true");
//            props.put("mail.smtp.port", SMTP_HOST_PORT);
//            props.put("mail.smtp.socketFactory.port", SMTP_HOST_PORT);
//            Session session = Session.getInstance(props, new Authenticator() {
//                protected PasswordAuthentication getPasswordAuthentication() {
//                    return new PasswordAuthentication(SMTP_AUTH_USER, SMTP_AUTH_PWD);
//                }
//            });
//
//            MimeMessage msg = new MimeMessage(session);
//            msg.setFrom(SMTP_AUTH_USER);
//            msg.setRecipients(MimeMessage.RecipientType.TO, receiver);
//            msg.setSubject(subject);
//            msg.setText(content);
//            msg.saveChanges();
//
////            Transport.send(msg);
//            Transport transport = session.getTransport("smtp");
//            transport.connect(SMTP_HOST_NAME, SMTP_HOST_PORT, SMTP_AUTH_USER, SMTP_AUTH_PWD);
//            transport.sendMessage(msg, msg.getAllRecipients());
//            transport.close();
//            return "success";
//        } catch (Exception e) {
//            e.printStackTrace();
//            return e.getMessage();
//        }
        return "failed";
    }

    public String sendVerificationCode(String receiver, String code) {
        String subject = "XynHub Verification Code";
        String content = """
                您的验证码是: %s
                请在 5 分钟内输入验证码完成登录。
                如果您没有进行登录操作，请忽略此邮件。    
                xynhub 🏳️‍⚧️ 🏳️‍🌈 团队敬上
               """.formatted(code);
        return sendMail(receiver, "xynhub 重置密码验证码", content);
    }

    public String sendStarNotification(String receiver, String fromUser, String repoName) {
        String subject = "XynHub Star Notification";
        String content = """
                您关注的仓库 %s 被 %s Star 了！
                xynhub 🏳️‍⚧️ 🏳️‍🌈 团队敬上
               """.formatted(repoName, fromUser);
        return sendMail(receiver, subject, content);
    }

    public String sendNewIssueNotification(List<String> receiver, String repoName, String issueTitle) {
        String subject = "XynHub New Issue Notification";
        String content = """
                您关注的仓库 %s 有新的 Issue %s 了！
                xynhub 🏳️‍⚧️ 🏳️‍🌈 团队敬上
               """.formatted(repoName, issueTitle);
        for (String to : receiver) {
            sendMail(to, subject, content);
        }
        return "success";
    }

    public String sendNewPrNotification(List<String> receiver, String repoName, String prTitle) {
        String subject = "XynHub New PR Notification";
        String content = """
                您的仓库 %s 有新的 PR %s 了！
                xynhub 🏳️‍⚧️ 🏳️‍🌈 团队敬上
               """.formatted(repoName, prTitle);
        for (String to : receiver) {
            sendMail(to, subject, content);
        }
        return "success";
    }

}
