package com.infy.pinterest.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;


import java.time.LocalDateTime;

@Entity
@Table(name = "blocked_users",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockedUser {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "block_id", length = 36)
    private String blockId;

    @Column(name = "blocker_id", nullable = false, length = 36)
    private String blockerId;

    @Column(name = "blocked_id", nullable = false, length = 36)
    private String blockedId;

    @Column(name = "blocked_at", updatable = false)
    private LocalDateTime blockedAt;

    @PrePersist
    protected void onCreate() {
        blockedAt = LocalDateTime.now();
    }
}
