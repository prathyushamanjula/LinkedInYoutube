package com.linkedinAppReview.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.linkedinAppReview.dto.YoutubeUser;
import com.linkedinAppReview.repository.YoutubeRepository;

@Component
public class YoutubeUserDao {
	
	@Autowired
	YoutubeRepository youtubeRepository;

	public void deleteUser(YoutubeUser deleteUser) {
		youtubeRepository.delete(deleteUser);
	}

	public YoutubeUser findById(int youtubeId) {
		return youtubeRepository.findById(youtubeId).orElse(null);
	}

}