package com.linkedinAppReview.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.linkedinAppReview.dto.LinkedInPageDto;

@Repository
public interface LinkedInPageRepository extends JpaRepository<LinkedInPageDto, Integer>{

	

	

}
