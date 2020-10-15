package dk.magenta.libreoffice.online.service;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Date;

/**
 * POJO to hold info about a WOPI access token.
 *
 * Created by seth on 30/04/16.
 */
public class WOPIAccessTokenInfo implements Serializable {

    private static final long serialVersionUID = 8344283129580208330L;

    private String accessToken;
    private Date issuedAt;
    private Date expiresAt;
    private String fileId;
    private String userName;

    public WOPIAccessTokenInfo(String accessToken, Date issuedAt, Date expiresAt, String fileId, String userName) {
        this.accessToken = accessToken;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.fileId = fileId;
        this.userName = userName;
    }

    /**
     * Return whether the access token has been issued and not expired at the
     * current time.
     *
     * @return
     */
    public boolean isValid() {
        return isValid(new Date());
    }

    /**
     * Return whether the access token is valid for the given date.
     * 
     * @return
     */
    public boolean isValid(Date when) {
        return when.after(issuedAt) && when.before(expiresAt);
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("accessToken", accessToken)
            .append("issuedAt", issuedAt)
            .append("expiresAt", expiresAt)
            .append("fileId", fileId)
            .append("userName", userName)
            .toString();
    }
}
