package com.infy.pinterest.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Table(name = "sponsored_pins")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SponsoredPin {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @Column(name = "sponsored_id", length = 36)
    private String sponsoredId;

    @Column(name = "pin_id", nullable = false, length = 36)
    private String pinId;

    @Column(name = "business_id", nullable = false, length = 36)
    private String businessId;

    @Column(name = "campaign_name", nullable = false, length = 100)
    private String campaignName;

    @Column(name = "budget", precision = 10, scale = 2)
    private BigDecimal budget = BigDecimal.ZERO;

    @Column(name = "spent", precision = 10, scale = 2)
    private BigDecimal spent = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PENDING;

    @Column(name = "impressions")
    private Integer impressions = 0;

    @Column(name = "clicks")
    private Integer clicks = 0;

    @Column(name = "saves")
    private Integer saves = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING, ACTIVE, PAUSED, COMPLETED
    }
}
