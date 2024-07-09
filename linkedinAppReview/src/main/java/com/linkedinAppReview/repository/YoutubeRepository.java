package com.linkedinAppReview.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.linkedinAppReview.dto.YoutubeUser;

@Repository
public interface YoutubeRepository extends JpaRepository<YoutubeUser, Integer> {

}