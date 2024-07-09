package com.linkedinAppReview.dao;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.linkedinAppReview.dto.LinkedInProfileDto;
import com.linkedinAppReview.repository.LinkedInProfileRepository;

@Service
public class LinkedInProfileDao {

	@Autowired
	LinkedInProfileRepository linkedInProfileRepository;
	
	public void saveProfile(LinkedInProfileDto linkedInProfileDto)
	{
		linkedInProfileRepository.save(linkedInProfileDto);
	}
	
	public Optional<LinkedInProfileDto> findProfileId(int id)
	{
		return linkedInProfileRepository.findById(id);
	}
	
	public Object deleteUser(LinkedInProfileDto linkedInProfileDto)
	{
		linkedInProfileRepository.delete(linkedInProfileDto);
		return linkedInProfileDto;
	}
}
