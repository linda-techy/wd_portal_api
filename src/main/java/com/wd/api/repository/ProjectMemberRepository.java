package com.wd.api.repository;

import com.wd.api.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findByProjectId(Long projectId);

    void deleteByProjectId(Long projectId);

    /** All projects a specific CustomerUser is linked to as a member (for the customer portal). */
    List<ProjectMember> findByCustomerUser_Id(Long customerUserId);

    /** Check if a CustomerUser is already a member of a specific project. */
    boolean existsByProject_IdAndCustomerUser_Id(Long projectId, Long customerUserId);
}
