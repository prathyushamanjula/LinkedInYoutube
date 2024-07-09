package com.linkedinAppReview.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.linkedinAppReview.dto.LinkedInPageDto;
import com.linkedinAppReview.repository.LinkedInPageRepository;

@Service
public class LinkedInPageDao {

	@Autowired
	LinkedInPageRepository linkedInPageRepository;
	
	public LinkedInPageDto save(LinkedInPageDto linkedInPageDto)
	{
		return linkedInPageRepository.save(linkedInPageDto);
	}

	public void deletePage(LinkedInPageDto linkedInPageDto) {
        linkedInPageRepository.delete(linkedInPageDto);
    }
	
}
