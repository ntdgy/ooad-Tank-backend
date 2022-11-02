package tank.ooad.fitgub.utils;


import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;

public class Crypto {

    private static final String salt = "fRaNkss";
    public static String hashPassword(String raw) {
        return getSHA1(getSHA1(raw) + salt);
    }

    public static String getSHA1(String raw) {
        return DigestUtils.sha1Hex(raw);
    }

    public static String generateVerificationCode(){
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append((int)(Math.random() * 10));
        }
        return code.toString();
    }
}
