package com.linkedinAppReview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.linkedinAppReview.dto.FaceBookUser;

public interface FacebookUserRepository extends JpaRepository<FaceBookUser, String>{
	public FaceBookUser findTopByOrderByFbIdDesc();
}
