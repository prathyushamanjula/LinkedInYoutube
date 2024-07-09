package com.linkedinAppReview.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.linkedinAppReview.dto.QuantumShareUser;

public interface QuantumShareUserRespository extends JpaRepository<QuantumShareUser, String>{
	
	public List<QuantumShareUser> findByEmailOrPhoneNo(String email, long phoneNo );

	public QuantumShareUser findTopByOrderByUserIdDesc();

	public QuantumShareUser findByVerificationToken(String token);

}
