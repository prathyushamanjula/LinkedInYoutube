package com.linkedinAppReview.dto;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Data;

@Data
@Component
@Entity
public class FaceBookUser {

	@Id
	private String fbId;
	private String fbuserId;
	private String fbuserUsername;
	private String firstName;
	private String lastName;
	private String email;
	private String birthday;
	private int noOfFbPages;

	@Column(length = 4000)
	private String pictureUrl;

	@Column(length = 2000)
	private String userAccessToken;

	@OneToMany(cascade=CascadeType.ALL)
//	@OneToMany
	private List<FacebookPageDetails> pageDetails;

}
