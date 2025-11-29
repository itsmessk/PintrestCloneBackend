package com.infy.pinterest.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;


import java.time.LocalDateTime;

@Entity
@Table(name = "board_collaborators",
        uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "user_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardCollaborator {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "collaborator_id", length = 36)
    private String collaboratorId;

    @Column(name = "board_id", nullable = false, length = 36)
    private String boardId;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Invitation.Permission permission = Invitation.Permission.EDIT;

    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
