package com.linkedinAppReview.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.linkedinAppReview.configure.JwtUtilConfig;
import com.linkedinAppReview.dao.QuantumShareUserDao;
import com.linkedinAppReview.response.ResponseStructure;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/quantum-share")
public class LinkedInPageController {
	
	@Autowired
	HttpServletRequest request;
	
	@Autowired
	ResponseStructure<String> structure;
	
	@Autowired
	JwtUtilConfig jwtUtilConfig;
	
	@Autowired
	QuantumShareUserDao userDao;
	
	
	
}
