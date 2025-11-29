package com.infy.pinterest.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;


import java.time.LocalDateTime;

@Entity
@Table(name = "invitations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "invitation_id", length = 36)
    private String invitationId;

    @Column(name = "board_id", nullable = false, length = 36)
    private String boardId;

    @Column(name = "from_user_id", nullable = false, length = 36)
    private String fromUserId;

    @Column(name = "to_user_id", nullable = false, length = 36)
    private String toUserId;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Permission permission = Permission.EDIT;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Permission {
        VIEW, EDIT
    }

    public enum Status {
        PENDING, ACCEPTED, DECLINED, IGNORED
    }
}
