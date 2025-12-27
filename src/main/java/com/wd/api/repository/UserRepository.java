package com.wd.api.repository;

import com.wd.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role.id = :roleId AND u.enabled = true")
    List<User> findByRoleId(@Param("roleId") Long roleId);

    // Alert system: Find all enabled users with specific role name
    @Query("SELECT u FROM User u WHERE u.role.name = :roleName AND u.enabled = true")
    List<User> findByRoleName(@Param("roleName") String roleName);
}