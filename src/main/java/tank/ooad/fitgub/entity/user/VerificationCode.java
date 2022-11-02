package tank.ooad.fitgub.entity.user;

public class VerificationCode {
    public int userId;
    public long expireTime;
    public String code;

    public VerificationCode(int userId, String code) {
        this.userId = userId;
        this.code = code;
        this.expireTime = System.currentTimeMillis() + 1000 * 60 * 5;
    }
}
