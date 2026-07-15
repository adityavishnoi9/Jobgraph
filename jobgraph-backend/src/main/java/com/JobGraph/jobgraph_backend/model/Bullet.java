package com.JobGraph.jobgraph_backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "bullets")
public class Bullet {

    public enum SourceType {
        experience, project
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SourceType sourceType;

    // Points to experience.id OR projects.id depending on sourceType.
    // Not a JPA @JoinColumn since it references two different tables.
    @Column(name = "source_id", nullable = false)
    private Integer sourceId;

    @Column(columnDefinition = "text", nullable = false)
    private String text;

    // Postgres text[]; List<String> mapping via Hibernate's array support
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> tags;

    // embedding VECTOR(1536) column is handled via native queries in the
    // matching-engine service, not mapped directly here to keep this
    // entity simple for the CRUD/profile stage.

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- Getters and setters ---

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public Integer getSourceId() { return sourceId; }
    public void setSourceId(Integer sourceId) { this.sourceId = sourceId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}