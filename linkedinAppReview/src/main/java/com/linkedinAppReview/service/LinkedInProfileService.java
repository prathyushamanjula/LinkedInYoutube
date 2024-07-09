package com.linkedinAppReview.service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinAppReview.dao.LinkedInPageDao;
import com.linkedinAppReview.dao.LinkedInProfileDao;
import com.linkedinAppReview.dao.QuantumShareUserDao;
import com.linkedinAppReview.dto.LinkedInPageDto;
import com.linkedinAppReview.dto.LinkedInProfileDto;
import com.linkedinAppReview.dto.QuantumShareUser;
import com.linkedinAppReview.dto.SocialAccounts;
import com.linkedinAppReview.response.ResponseStructure;

@Service
public class LinkedInProfileService {

	@Value("${linkedin.clientId}")
    private String clientId;

    @Value("${linkedin.clientSecret}")
    private String clientSecret;

    @Value("${linkedin.redirectUri}")
    private String redirectUri;

    @Value("${linkedin.scope}")
    private String scope;
    
    @Autowired
    LinkedInProfileDto linkedInProfileDto;
    
    @Autowired
    LinkedInProfileDao linkedInProfileDao;
    
    @Autowired
    LinkedInPageDto linkedInPageDto;
    
    @Autowired
    LinkedInPageDao linkedInPageDao;
    
     @Autowired
	ResponseStructure<String> structure;
    
    @Autowired
    private RestTemplate restTemplate;
    
   

    HttpHeaders httpHeaders = new HttpHeaders();
   
    @Autowired
	QuantumShareUserDao userDao;
    
    public String generateAuthorizationUrl() {
        return "https://www.linkedin.com/oauth/v2/authorization" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=" + scope;
    }
    
    public String exchangeAuthorizationCodeForAccessToken(String code) throws IOException {
    	 String url = "https://www.linkedin.com/oauth/v2/accessToken";

         HttpHeaders headers = new HttpHeaders();
         headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

         MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
         body.add("grant_type", "authorization_code");
         body.add("code", code);
         body.add("client_id", clientId);
         body.add("client_secret", clientSecret);
         body.add("redirect_uri", redirectUri);

         HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

         RestTemplate restTemplate = new RestTemplate();
         ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

         Map<String, Object> responseBody = responseEntity.getBody();
         return responseBody != null ? (String) responseBody.get("access_token") : null;
 
    }
    
    private String parseResponse(HttpURLConnection connection) throws IOException {
        try (Scanner scanner = new Scanner(connection.getInputStream())) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    private String parseAccessToken(String response) throws IOException {
        return new ObjectMapper().readTree(response).get("access_token").asText();
    }

    public ResponseEntity<ResponseStructure<String>> getUserInfoWithToken(String code,QuantumShareUser user) throws IOException {
        String accessToken = exchangeAuthorizationCodeForAccessToken(code);
        
        System.out.println("AT = " + accessToken);

        if (accessToken == null) {
            structure.setCode(500);
            structure.setMessage("Failed to retrieve access token");
            structure.setStatus("error");
            structure.setPlatform("LinkedIn");
            structure.setData(null);
            return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ResponseEntity<String> organizationAclsResponse = getProfileInfo(accessToken,user);
        
        

        if (organizationAclsResponse.getStatusCode() == HttpStatus.OK) {
            structure.setCode(200);
            structure.setMessage("LinkedIn Profile Connected Successfully");
            structure.setStatus("success");
            structure.setPlatform("LinkedIn");
            structure.setData(organizationAclsResponse.getBody());
            return new ResponseEntity<>(structure, HttpStatus.OK);
        } else {
            structure.setCode(organizationAclsResponse.getStatusCode().value());
            structure.setMessage("Failed to retrieve organization info");
            structure.setStatus("error");
            structure.setPlatform("LinkedIn");
            structure.setData(null);
            return new ResponseEntity<>(structure, organizationAclsResponse.getStatusCode());
        }
    }

    

    public ResponseEntity<String> getProfileInfo(String accessToken, QuantumShareUser user) {
        String userInfoUrl = "https://api.linkedin.com/v2/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(userInfoUrl, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            String responseBody = response.getBody();

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);
                String localizedFirstName = rootNode.path("localizedFirstName").asText();
                String localizedLastName = rootNode.path("localizedLastName").asText();
                String id = rootNode.path("id").asText();

                String username = localizedFirstName + " " + localizedLastName;

                // Fetch profile image URL
                ResponseStructure<String> profileImageResponse = getLinkedInProfile(accessToken);
                String imageUrl = null;

                if (profileImageResponse.getStatus().equals("Success")) {
                    Object data = profileImageResponse.getData();
                    if (data instanceof String) {
                        imageUrl = (String) data;
                    } else {
                        // Handle the case where data is not a String
                        imageUrl = "default_image_url"; // or handle differently as per your application's logic
                    }
                } else {
                    // Handle case where profile image fetch failed
                    imageUrl = "default_image_url"; // or handle differently as per your application's logic
                }


                // Construct custom response
                String customResponse = "{ \"profile_sub\": \"" + id + "\", \"LinkedInUsername\": \"" + username + "\", \"access_token\": \"" + accessToken + "\", \"image_url\": \"" + imageUrl + "\" }";

                LinkedInProfileDto linkedInProfileDto = new LinkedInProfileDto();
                linkedInProfileDto.setLinkedinProfileURN(id);
                linkedInProfileDto.setLinkedinProfileUserName(username);
                linkedInProfileDto.setLinkedinProfileAccessToken(accessToken);
                linkedInProfileDto.setLinkedinProfileImage(imageUrl); // Set profile image URL

                // Save or update LinkedIn profile info
                SocialAccounts socialAccounts = user.getSocialAccounts();
                if (socialAccounts == null) {
                    socialAccounts = new SocialAccounts();
                    socialAccounts.setLinkedInProfileDto(linkedInProfileDto);
                    user.setSocialAccounts(socialAccounts);
                } else {
                    if (socialAccounts.getLinkedInProfileDto() == null) {
                        socialAccounts.setLinkedInProfileDto(linkedInProfileDto);
                    }
                }

                userDao.saveUser(user);

                HttpHeaders customHeaders = new HttpHeaders();
                customHeaders.setContentType(MediaType.APPLICATION_JSON);

                return new ResponseEntity<>(customResponse, customHeaders, HttpStatus.OK);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while processing the request: " + e.getMessage());
            }
        } else {
            return response;
        }
    }


    public ResponseStructure<String> getLinkedInProfile(String accessToken) {
        ResponseStructure<String> responseStructure = new ResponseStructure<>();
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> entity = new HttpEntity<>("", headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    "https://api.linkedin.com/v2/me?projection=(id,profilePicture(displayImage~:playableStreams))",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            String imageUrl = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"; // default image URL

            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode elements = rootNode.path("profilePicture").path("displayImage~").path("elements");

                if (elements.isArray() && elements.size() > 0) {
                    for (JsonNode element : elements) {
                        JsonNode displaySize = element.path("data").path("com.linkedin.digitalmedia.mediaartifact.StillImage").path("displaySize");
                        if (displaySize.path("width").asInt() == 200 && displaySize.path("height").asInt() == 200) {
                            JsonNode identifiers = element.path("identifiers");
                            if (identifiers.isArray() && identifiers.size() > 0) {
                                String fetchedImageUrl = identifiers.get(0).path("identifier").asText();
                                if (fetchedImageUrl != null && !fetchedImageUrl.isEmpty()) {
                                    imageUrl = fetchedImageUrl; // use fetched image URL if available
                                    break;
                                }
                            }
                        }
                    }
                }

                responseStructure.setStatus("Success");
                responseStructure.setMessage("Profile fetched successfully");
                responseStructure.setCode(HttpStatus.OK.value());
                responseStructure.setData(imageUrl);
            } else {
                responseStructure.setStatus("Failure");
                responseStructure.setMessage("Failed to fetch profile: " + response.getStatusCode());
                responseStructure.setCode(response.getStatusCode().value());
                responseStructure.setData(imageUrl); // return default image URL even if response status is not OK
            }
        } catch (RestClientException e) {
            responseStructure.setStatus("Failure");
            responseStructure.setMessage("Exception occurred: " + e.getMessage());
            responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return default image URL in case of exception
        } catch (IOException e) {
            responseStructure.setStatus("Failure");
            responseStructure.setMessage("Exception occurred: " + e.getMessage());
            responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return default image URL in case of exception
        }

        return responseStructure;
    }

	
	
	 public ResponseEntity<ResponseStructure<List<LinkedInPageDto>>> getOrganizationsDetailsByProfile(String code, QuantumShareUser user) throws IOException {
	        String accessToken = exchangeAuthorizationCodeForAccessToken(code);
	        return getOrganizationInfo(accessToken);
	    }
  
	 public ResponseEntity<ResponseStructure<List<LinkedInPageDto>>> getOrganizationInfo(String accessToken) {
		    System.out.println("AccessToken = " + accessToken);
		    HttpHeaders headers = new HttpHeaders();
		    headers.set("X-Restli-Protocol-Version", "2.0.0");
		    headers.set("Authorization", "Bearer " + accessToken);
		    HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

		    ResponseEntity<String> responseEntity;

		    try {
		        responseEntity = restTemplate.exchange(
		            "https://api.linkedin.com/v2/organizationAcls?q=roleAssignee",
		            HttpMethod.GET,
		            requestEntity,
		            String.class
		        );
		    } catch (Exception e) {
		        ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		        structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		        structure.setMessage("Failed to make request");
		        structure.setStatus("error");
		        structure.setPlatform("LinkedIn");
		        structure.setData(null);
		        return new ResponseEntity<>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
		    }

		    if (responseEntity.getStatusCode() == HttpStatus.OK) {
		        String responseBody = responseEntity.getBody();
		        ObjectMapper objectMapper = new ObjectMapper();
		        try {
		            JsonNode rootNode = objectMapper.readTree(responseBody);
		            JsonNode elementsNode = rootNode.path("elements");

		            if (elementsNode.isEmpty()) {
		                ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		                structure.setCode(HttpStatus.OK.value());
		                structure.setMessage("User does not have associated pages");
		                structure.setStatus("success");
		                structure.setPlatform("LinkedIn");
		                structure.setData(null);
		                return ResponseEntity.ok(structure);
		            } else {
		                List<String> organizationUrns = new ArrayList<>();
		                for (JsonNode pageNode : elementsNode) {
		                    String organizationURN = pageNode.path("organization").asText();
		                    organizationUrns.add(organizationURN);
		                }

		                // Get the organization names
		                List<String> organizationNames = getPageNamesFromLinkedInAPI(accessToken, organizationUrns);

		                // Prepare LinkedInPageDto objects
		                List<LinkedInPageDto> data = new ArrayList<>();
		                for (int i = 0; i < organizationUrns.size(); i++) {
		                    LinkedInPageDto linkedInPageDto = new LinkedInPageDto();
		                    linkedInPageDto.setLinkedinPageURN(organizationUrns.get(i));
		                    linkedInPageDto.setLinkedinPageAccessToken(accessToken);
		                    linkedInPageDto.setLinkedinPageName(organizationNames.get(i));
		                    data.add(linkedInPageDto);
		                }

		                // Prepare response structure
		                ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		                structure.setCode(HttpStatus.OK.value());
		                structure.setMessage("LinkedIn associated pages");
		                structure.setStatus("success");
		                structure.setPlatform("LinkedIn");
		                structure.setData(data);
		                return ResponseEntity.ok(structure);
		            }
		        } catch (JsonProcessingException e) {
		            ResponseStructure<List<LinkedInPageDto>> errorResponse = new ResponseStructure<>();
		            errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		            errorResponse.setMessage("Failed to process JSON response");
		            errorResponse.setStatus("error");
		            errorResponse.setPlatform("LinkedIn");
		            errorResponse.setData(null);
		            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
		        }
		    } else {
		        ResponseStructure<List<LinkedInPageDto>> structure = new ResponseStructure<>();
		        structure.setCode(responseEntity.getStatusCode().value());
		        structure.setMessage("Failed to retrieve organization info");
		        structure.setStatus("error");
		        structure.setPlatform("LinkedIn");
		        structure.setData(null);
		        return new ResponseEntity<>(structure, responseEntity.getStatusCode());
		    }
		}



	 private List<String> getPageNamesFromLinkedInAPI(String accessToken, List<String> organizationUrns) {
		    List<String> pageNames = new ArrayList<>();

		    HttpHeaders headers = new HttpHeaders();
		    headers.set("Authorization", "Bearer " + accessToken);
		    HttpEntity<String> entity = new HttpEntity<>(headers);

		    for (String organizationUrn : organizationUrns) {
		        String organizationId = organizationUrn.substring(organizationUrn.lastIndexOf(':') + 1);
		        
		        try {
		            ResponseEntity<String> responseEntity = restTemplate.exchange(
		                    "https://api.linkedin.com/v2/organizations/" + organizationId,
		                    HttpMethod.GET,
		                    entity,
		                    String.class
		            );

		            if (responseEntity.getStatusCode() == HttpStatus.OK) {
		                ObjectMapper objectMapper = new ObjectMapper();
		                JsonNode rootNode = objectMapper.readTree(responseEntity.getBody());
		                String pageName = rootNode.path("localizedName").asText();
		                pageNames.add(pageName);
		            } else {
		                pageNames.add(null); // Add null for failed requests
		            }
		        } catch (Exception e) {
		            pageNames.add(null); // Add null for exceptions
		            e.printStackTrace();
		        }
		    }

		    return pageNames;
		}

	 public ResponseEntity<ResponseStructure<Map<String, Object>>> saveSelectedPage(LinkedInPageDto selectedLinkedInPageDto, QuantumShareUser user) {
		    // Check if the user is authenticated
		    if (user == null) {
		        ResponseStructure<Map<String, Object>> response = new ResponseStructure<>();
		        response.setCode(HttpStatus.UNAUTHORIZED.value());
		        response.setMessage("User not authenticated");
		        response.setStatus("error");
		        response.setData(Collections.emptyMap()); // Empty data for error case
		        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
		    }

		    // Check if the selected page DTO is valid
		    if (selectedLinkedInPageDto == null || selectedLinkedInPageDto.getLinkedinPageURN() == null 
		            || selectedLinkedInPageDto.getLinkedinPageName() == null 
		            || selectedLinkedInPageDto.getLinkedinPageAccessToken() == null) {
		        ResponseStructure<Map<String, Object>> response = new ResponseStructure<>();
		        response.setCode(HttpStatus.BAD_REQUEST.value());
		        response.setMessage("Invalid selected page data");
		        response.setStatus("error");
		        response.setData(Collections.emptyMap()); // Empty data for error case
		        return ResponseEntity.badRequest().body(response);
		    }

		    // Extract only the value from the URN
		    String organizationId = selectedLinkedInPageDto.getLinkedinPageURN().substring(
		        selectedLinkedInPageDto.getLinkedinPageURN().lastIndexOf(':') + 1);

		    // Fetch the organization logo URL and network size concurrently
		    ResponseStructure<String> logoResponse = getOrganizationLogo(selectedLinkedInPageDto.getLinkedinPageAccessToken(), organizationId);
		    ResponseStructure<Integer> networkSizeResponse = getNetworkSize(selectedLinkedInPageDto.getLinkedinPageAccessToken(), organizationId);

		    // Check if both responses are successful
		    if (logoResponse.getCode() == HttpStatus.OK.value() && networkSizeResponse.getCode() == HttpStatus.OK.value()) {
		        // Logo URL and network size fetched successfully
		        String logoUrl = (String) logoResponse.getData();
		        Integer firstDegreeSize = (Integer) networkSizeResponse.getData();

		        // Save logo URL into selectedLinkedInPageDto
		        selectedLinkedInPageDto.setLinkedinPageImage(logoUrl);
		        selectedLinkedInPageDto.setLinkedinPageFollowers(firstDegreeSize);

		        // Create a new social accounts object if it doesn't exist
		        SocialAccounts socialAccounts = user.getSocialAccounts();
		        if (socialAccounts == null) {
		            socialAccounts = new SocialAccounts();
		        }

		        // Retrieve or create the LinkedIn profile DTO
		        LinkedInProfileDto linkedInProfileDto = socialAccounts.getLinkedInProfileDto();
		        if (linkedInProfileDto == null) {
		            linkedInProfileDto = new LinkedInProfileDto();
		            socialAccounts.setLinkedInProfileDto(linkedInProfileDto);
		        }

		        // Set the extracted organization ID back to the DTO
		        selectedLinkedInPageDto.setLinkedinPageURN(organizationId);

		        // Associate the selected page with the LinkedIn profile
		        selectedLinkedInPageDto.setProfile(linkedInProfileDto);

		        // Add the selected page to the profile's list of pages
		        List<LinkedInPageDto> pages = linkedInProfileDto.getPages();
		        if (pages == null) {
		            pages = new ArrayList<>();
		        }
		        pages.add(selectedLinkedInPageDto);
		        linkedInProfileDto.setPages(pages);

		        // Update the user's social accounts
		        user.setSocialAccounts(socialAccounts);

		        // Save the updated user
		        userDao.saveUser(user);

		        // Prepare success response with both logoUrl and firstDegreeSize
		        ResponseStructure<Map<String, Object>> response = new ResponseStructure<>();
		        response.setCode(HttpStatus.OK.value());
		        response.setMessage(selectedLinkedInPageDto.getLinkedinPageName());
		        response.setStatus("success");
		        response.setPlatform("LinkedIn");

		        Map<String, Object> responseData = new HashMap<>();
		        responseData.put("logoUrl", logoUrl);
		        responseData.put("followers_count", firstDegreeSize); 

		        response.setData(responseData);

		        return ResponseEntity.ok(response);
		    } else {
		        // Handle failure to fetch logo URL or network size
		        int errorCode = (logoResponse.getCode() != HttpStatus.OK.value()) ? logoResponse.getCode() : networkSizeResponse.getCode();
		        String errorMessage = (logoResponse.getCode() != HttpStatus.OK.value()) ? logoResponse.getMessage() : networkSizeResponse.getMessage();

		        ResponseStructure<Map<String, Object>> response = new ResponseStructure<>();
		        response.setCode(errorCode);
		        response.setMessage("Failed to fetch organization details: " + errorMessage);
		        response.setStatus("error");
		        response.setData(Collections.emptyMap()); // Empty data for error case
		        return ResponseEntity.status(errorCode).body(response);
		    }
		}

	 
	 public ResponseStructure<Integer> getNetworkSize(String accessToken, String organizationId) {
	        HttpHeaders headers = new HttpHeaders();
	        headers.setBearerAuth(accessToken);
	        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

	        HttpEntity<String> entity = new HttpEntity<>(headers);

	        String url = "https://api.linkedin.com/v2/networkSizes/urn:li:organization:" + organizationId + "?edgeType=CompanyFollowedByMember";
	        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

	        ResponseStructure<Integer> responseStructure = new ResponseStructure<>();
	        if (response.getStatusCode().is2xxSuccessful()) {
	            try {
	                // Parse JSON response to get firstDegreeSize
	                ObjectMapper mapper = new ObjectMapper();
	                JsonNode rootNode = mapper.readTree(response.getBody());
	                Integer firstDegreeSize = rootNode.path("firstDegreeSize").asInt();

	                responseStructure.setMessage("Network size retrieved successfully");
	                responseStructure.setStatus("OK");
	                responseStructure.setCode(response.getStatusCode().value());
	                responseStructure.setPlatform("LinkedIn");
	                responseStructure.setData(firstDegreeSize);

	                return responseStructure;
	            } catch (JsonProcessingException e) {
	                // Handle JSON parsing exception
	                responseStructure.setMessage("Failed to parse network size response");
	                responseStructure.setStatus("Error");
	                responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
	                responseStructure.setPlatform("LinkedIn");
	                responseStructure.setData(null);
	            }
	        } else {
	            responseStructure.setMessage("Failed to retrieve network size");
	            responseStructure.setStatus("Error");
	            responseStructure.setCode(response.getStatusCode().value());
	            responseStructure.setPlatform("LinkedIn");
	            responseStructure.setData(null);
	        }

	        return responseStructure;
	    }

	 public ResponseStructure<String> getOrganizationLogo(String accessToken, String organizationId) {
		    ResponseStructure<String> responseStructure = new ResponseStructure<>();
		    RestTemplate restTemplate = new RestTemplate();

		    HttpHeaders headers = new HttpHeaders();
		    headers.setContentType(MediaType.APPLICATION_JSON);
		    headers.set("Authorization", "Bearer " + accessToken);
		    headers.set("LinkedIn-Version", "202405");
		    headers.set("X-Restli-Protocol-Version", "2.0.0");

		    HttpEntity<String> entity = new HttpEntity<>("", headers);

		    try {
		        ResponseEntity<String> response = restTemplate.exchange(
		                "https://api.linkedin.com/v2/organizations/" + organizationId + "?projection=(logoV2(original~:playableStreams,cropped~:playableStreams,cropInfo))",
		                HttpMethod.GET,
		                entity,
		                String.class
		        );

		        String logoUrl = "https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"; // default image URL

		        if (response.getStatusCode() == HttpStatus.OK) {
		            ObjectMapper objectMapper = new ObjectMapper();
		            JsonNode rootNode = objectMapper.readTree(response.getBody());

		            JsonNode elementsNode = rootNode.path("logoV2").path("original~").path("elements");
		            if (elementsNode.isArray() && elementsNode.size() > 0) {
		                JsonNode firstElement = elementsNode.get(0);
		                JsonNode identifiersNode = firstElement.path("identifiers");
		                if (identifiersNode.isArray() && identifiersNode.size() > 0) {
		                    String fetchedLogoUrl = identifiersNode.get(0).path("identifier").asText();
		                    
		                    if (fetchedLogoUrl != null && !fetchedLogoUrl.isEmpty()) {
		                        logoUrl = fetchedLogoUrl; // use fetched logo URL if available
		                    }
		                }
		            }

		            responseStructure.setStatus("Success");
		            responseStructure.setMessage("Organization logo fetched successfully");
		            responseStructure.setCode(HttpStatus.OK.value());
		            responseStructure.setData(logoUrl);
		        } else {
		            responseStructure.setStatus("Failure");
		            responseStructure.setMessage("Failed to fetch organization logo: " + response.getStatusCode());
		            responseStructure.setCode(response.getStatusCode().value());
		            responseStructure.setData(logoUrl); // return default image URL even if response status is not OK
		        }
		    } catch (Exception e) {
		        responseStructure.setStatus("Failure");
		        responseStructure.setMessage("Exception occurred: " + e.getMessage());
		        responseStructure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
		        responseStructure.setData("https://quantumshare.quantumparadigm.in/vedio/ProfilePicture.jpg"); // return default image URL in case of exception
		    }

		    return responseStructure;
		}

} 
