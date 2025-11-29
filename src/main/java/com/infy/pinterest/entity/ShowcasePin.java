package com.infy.pinterest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
@Entity
@Table(name = "showcase_pins")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowcasePin {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "showcase_pin_id", length = 36)
    private String showcasePinId;

    @Column(name = "showcase_id", nullable = false, length = 36)
    private String showcaseId;

    @Column(name = "pin_id", nullable = false, length = 36)
    private String pinId;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
