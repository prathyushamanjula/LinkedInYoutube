package com.linkedinAppReview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.linkedinAppReview.dto.FacebookPageDetails;

public interface FacebookPageRepository extends JpaRepository<FacebookPageDetails, Integer> {

	public FacebookPageDetails findTopByOrderByPageTableIdDesc();
}