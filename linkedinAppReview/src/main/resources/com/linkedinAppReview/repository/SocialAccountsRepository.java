package com.linkedinAppReview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.linkedinAppReview.dto.SocialAccounts;

public interface SocialAccountsRepository extends JpaRepository<SocialAccounts, Integer> {

}
