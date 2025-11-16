package com.wd.api.repository;

import com.wd.api.model.PartnershipUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartnershipUserRepository extends JpaRepository<PartnershipUser, Long> {
    
    Optional<PartnershipUser> findByPhone(String phone);
    
    Optional<PartnershipUser> findByEmail(String email);
    
    Optional<PartnershipUser> findByPhoneOrEmail(String phone, String email);
    
    boolean existsByPhone(String phone);
    
    boolean existsByEmail(String email);
}

