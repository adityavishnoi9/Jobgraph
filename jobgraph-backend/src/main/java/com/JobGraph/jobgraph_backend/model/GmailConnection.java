package com.JobGraph.jobgraph_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "gmail_connections")
// Each account can connect one Gmail inbox at a time.
public class GmailConnection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;
    @Column(nullable = false, length = 320)
    private String gmailAddress;
    @Column(nullable = false, columnDefinition = "text")
    // The refresh token is encrypted before it reaches this database column.
    private String encryptedRefreshToken;
    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Integer getId() { return id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public String getGmailAddress() { return gmailAddress; }
    public void setGmailAddress(String gmailAddress) { this.gmailAddress = gmailAddress; }
    public String getEncryptedRefreshToken() { return encryptedRefreshToken; }
    public void setEncryptedRefreshToken(String encryptedRefreshToken) { this.encryptedRefreshToken = encryptedRefreshToken; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
