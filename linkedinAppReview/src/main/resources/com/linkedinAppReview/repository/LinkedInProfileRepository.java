package com.linkedinAppReview.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.linkedinAppReview.dto.LinkedInProfileDto;

@Repository
public interface LinkedInProfileRepository extends JpaRepository<LinkedInProfileDto, Integer> {

}
