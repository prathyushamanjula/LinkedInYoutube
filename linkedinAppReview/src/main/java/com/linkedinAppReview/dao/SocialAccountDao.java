package com.linkedinAppReview.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.linkedinAppReview.dto.SocialAccounts;
import com.linkedinAppReview.repository.SocialAccountsRepository;

@Component
public class SocialAccountDao {

	@Autowired
	SocialAccountsRepository accountsRepository;

	public void save(SocialAccounts accounts) {
		accountsRepository.save(accounts);
	}

	public void deleteSocialAccount(SocialAccounts socialAccounts) {
		accountsRepository.delete(socialAccounts);
		
	}
	
	

}
