package com.infy.pinterest.repository;



import com.infy.pinterest.entity.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserReportRepository extends JpaRepository<UserReport, String> {

    Page<UserReport> findByReporterId(String reporterId, Pageable pageable);

    Page<UserReport> findByReportedUserId(String reportedUserId, Pageable pageable);

    Page<UserReport> findByStatus(UserReport.Status status, Pageable pageable);
}
