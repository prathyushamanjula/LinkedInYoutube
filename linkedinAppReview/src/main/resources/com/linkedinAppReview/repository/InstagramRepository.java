package com.linkedinAppReview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.linkedinAppReview.dto.InstagramUser;

public interface InstagramRepository extends JpaRepository<InstagramUser, String> {
	public InstagramUser findTopByOrderByInstaIdDesc();
}
