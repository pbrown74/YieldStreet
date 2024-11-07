package com.yieldstreet.repository;

import com.yieldstreet.entity.Accreditation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccreditationRepository extends JpaRepository<Accreditation, String> {

    List<Accreditation> findByUserIdIs(String userId);

}