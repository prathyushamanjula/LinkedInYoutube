package com.linkedinAppReview.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedinAppReview.configure.ConfigurationClass;
import com.linkedinAppReview.dao.QuantumShareUserDao;
import com.linkedinAppReview.dto.MediaPost;
import com.linkedinAppReview.dto.QuantumShareUser;
import com.linkedinAppReview.dto.SocialAccounts;
import com.linkedinAppReview.dto.YoutubeUser;
import com.linkedinAppReview.exception.CommonException;
import com.linkedinAppReview.response.ErrorResponse;
import com.linkedinAppReview.response.ResponseStructure;
import com.linkedinAppReview.response.ResponseWrapper;
import com.linkedinAppReview.response.SuccessResponse;

@Service
public class YoutubeService {

	@Value("${youtube.client-id}")
	private String clientId;

	@Value("${youtube.client-secret}")
	private String clientSecret;

	@Value("${youtube.redirect-uri}")
	private String redirectUri;

	@Value("${youtube.token-uri}")
	private String tokenUri;

	@Value("${youtube.channel-details-uri}")
	private String channelDetailsUri;

	@Value("${youtube.auth-uri}")
	private String authUri;

	@Value("${youtube.scope}")
	private String scope;

	@Autowired
	QuantumShareUserDao userDao;

	@Autowired
	ResponseStructure<String> structure;

	@Autowired
	SuccessResponse successResponse;

	@Autowired
	ErrorResponse errorResponse;

	@Autowired
	HttpHeaders headers;

	@Autowired
	ConfigurationClass config;

	@Autowired
	MultiValueMap<String, Object> multiValueMap;

	@Autowired
	RestTemplate restTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	YoutubeUser youtubeUser;

	@Autowired
	SocialAccounts socialAccounts;

	@Autowired
	MultiValueMap<String, Object> linkedMultiValueMap;

	@Autowired
	ConfigurationClass.ByteArrayResourceFactory byteArrayResourceFactory;

	public ResponseEntity<ResponseStructure<String>> getAuthorizationUrl(QuantumShareUser user) {
		String authUri = "https://accounts.google.com/o/oauth2/v2/auth";
		String ouath = authUri + "?response_type=code&client_id=" + clientId + "&redirect_uri=" + redirectUri
				+ "&scope=" + scope + "&access_type=" + "offline" + "&prompt=" + "consent";
		System.out.println(ouath);
		structure.setCode(HttpStatus.OK.value());
		structure.setStatus("success");
		structure.setMessage("oauth_url generated successfully");
		structure.setPlatform(null);
		structure.setData(ouath);
		return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
	}

	public ResponseEntity<ResponseStructure<String>> verifyToken(String code, QuantumShareUser user) {
		try {
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			multiValueMap.add("code", code);
			multiValueMap.add("client_id", clientId);
			multiValueMap.add("client_secret", clientSecret);
			multiValueMap.add("redirect_uri", redirectUri);
			multiValueMap.add("grant_type", "authorization_code");

			HttpEntity<MultiValueMap<String, Object>> httpRequest = config.getHttpEntityWithMap(multiValueMap, headers);
			System.out.println("request : " + httpRequest + "   -   " + httpRequest.toString());
			ResponseEntity<String> response = restTemplate.exchange(tokenUri, HttpMethod.POST, httpRequest,
					String.class);
			System.out.println("response  :  " + response);
			if (response.getStatusCode() == HttpStatus.OK) {
				JsonNode responseBody = objectMapper.readTree(response.getBody());
				if (responseBody != null && responseBody.has("access_token")) {
					String accessToken = responseBody.get("access_token").asText();
					String youtubeUserDetails = getChannelDetails(accessToken);
					return saveYoutubeUser(youtubeUserDetails, user, accessToken);
				} else {
					throw new CommonException("Access token not found in response");
				}
			} else {
				structure.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
				structure.setData(null);
				structure.setMessage("Something went wrong!!");
				structure.setPlatform(null);
				structure.setStatus("error");
				return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		} catch (Exception e) {
			throw new CommonException(e.getMessage());
		}
	}

	private String getChannelDetails(String accessToken) {
		try {
			String url = "https://www.googleapis.com/youtube/v3/channels?mine=true&part=snippet,statistics&access_token="
					+ accessToken;
			headers.setBearerAuth(accessToken);
			HttpEntity<String> httpRequest = config.getHttpEntity(headers);
			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, httpRequest, String.class);
			if (response.getStatusCode().is2xxSuccessful()) {
				return response.getBody();
			} else {
				return null;
			}
		} catch (NullPointerException exception) {
			throw new CommonException(exception.getMessage());
		}
	}

	private ResponseEntity<ResponseStructure<String>> saveYoutubeUser(String youtubeUserDetails, QuantumShareUser user,
			String accessToken) {
		try {
			JsonNode rootNode = objectMapper.readTree(youtubeUserDetails);
			SocialAccounts accounts = user.getSocialAccounts();
			if (accounts == null) {
				JsonNode itemsNode = rootNode.path("items").get(0);
				JsonNode snippetNode = itemsNode.path("snippet");
				youtubeUser.setYoutubeUserAccessToken(accessToken);
				youtubeUser.setYoutubeChannelId(itemsNode.path("id").asText());
				youtubeUser.setChannelName(snippetNode.path("title").asText());
				youtubeUser.setSubscriberCount(itemsNode.path("statistics").path("subscriberCount").asInt());
				youtubeUser.setChannelImageUrl(snippetNode.path("thumbnails").path("default").path("url").asText());
				socialAccounts.setYoutubeUser(youtubeUser);
				user.setSocialAccounts(socialAccounts);
			} else if (accounts.getYoutubeUser() == null) {
				JsonNode itemsNode = rootNode.path("items").get(0);
				JsonNode snippetNode = itemsNode.path("snippet");
				youtubeUser.setYoutubeUserAccessToken(accessToken);
				youtubeUser.setYoutubeChannelId(itemsNode.path("id").asText());
				youtubeUser.setChannelName(snippetNode.path("title").asText());
				youtubeUser.setSubscriberCount(itemsNode.path("statistics").path("subscriberCount").asInt());
				youtubeUser.setChannelImageUrl(snippetNode.path("thumbnails").path("default").path("url").asText());
				accounts.setYoutubeUser(youtubeUser);
				user.setSocialAccounts(accounts);
			} else {
				YoutubeUser ytUser = accounts.getYoutubeUser();
				JsonNode itemsNode = rootNode.path("items").get(0);
				JsonNode snippetNode = itemsNode.path("snippet");
				ytUser.setYoutubeUserAccessToken(accessToken);
				ytUser.setYoutubeChannelId(itemsNode.path("id").asText());
				ytUser.setChannelName(snippetNode.path("title").asText());
				ytUser.setSubscriberCount(itemsNode.path("statistics").path("subscriberCount").asInt());
				ytUser.setChannelImageUrl(snippetNode.path("thumbnails").path("default").path("url").asText());
				accounts.setYoutubeUser(ytUser);
				user.setSocialAccounts(accounts);
			}
			userDao.save(user);
			structure.setCode(HttpStatus.OK.value());
			structure.setStatus("success");
			structure.setMessage("Youtube Connected Successfully");
			structure.setPlatform("youtube");

			YoutubeUser yUser = user.getSocialAccounts().getYoutubeUser();
			Map<String, Object> map = config.getMap();
			map.put("channelName", yUser.getChannelName());
			map.put("subscriberCount", yUser.getSubscriberCount());
			map.put("channelImageUrl", yUser.getChannelImageUrl());
			structure.setData(map);
			return new ResponseEntity<ResponseStructure<String>>(structure, HttpStatus.OK);
		} catch (NullPointerException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonMappingException e) {
			throw new CommonException(e.getMessage());
		} catch (JsonProcessingException e) {
			throw new CommonException(e.getMessage());
		}
	}

	// Media Posting
	public ResponseEntity<ResponseWrapper> postMediaToChannel(MediaPost mediaPost, MultipartFile mediaFile,
			YoutubeUser youtubeUser) {
		System.out.println("Coming to Youtube Service");
		if (youtubeUser == null) {
			structure.setMessage("Youtube user not found");
			structure.setCode(HttpStatus.NOT_FOUND.value());
			structure.setPlatform("youtube");
			structure.setStatus("error");
			structure.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure), HttpStatus.NOT_FOUND);
		}
		int youtubeId = youtubeUser.getYoutubeId();
		String youtubeChannelId = youtubeUser.getYoutubeChannelId();
		String contentType = mediaFile.getContentType();
		try {
			if (contentType != null && contentType.startsWith("video/")) {
				System.out.println("Send video to Channel");
				sendVideoToChannel(youtubeId, youtubeChannelId, mediaFile, mediaPost.title, mediaPost.getCaption());
			} else {
				structure.setMessage("Youtube:Unsupported media type");
				structure.setCode(HttpStatus.BAD_REQUEST.value());
				structure.setPlatform("youtube");
				structure.setStatus("error");
				structure.setData(null);
				return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(structure),
						HttpStatus.BAD_REQUEST);
			}
			successResponse.setMessage("Posted On Youtube");
			successResponse.setCode(HttpStatus.OK.value());
			successResponse.setPlatform("youtube");
			successResponse.setStatus("success");
			successResponse.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(successResponse), HttpStatus.OK);
		} catch (Exception e) {
			errorResponse.setMessage("Failed to send media: " + e.getMessage());
			errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
			errorResponse.setPlatform("youtube");
			errorResponse.setStatus("error");
			errorResponse.setData(null);
			return new ResponseEntity<ResponseWrapper>(config.getResponseWrapper(errorResponse),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public void sendVideoToChannel(int youtubeId, String youtubeChannelId, MultipartFile mediaFile, String title, String caption)
			throws IOException {
		System.out.println("Coming to sendVideoToChannel method");

		String uploadUrl = "https://www.googleapis.com/upload/youtube/v3/videos?part=snippet";

		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		String snippetJson = "{\"snippet\": {" + "\"channelId\": \"" + youtubeChannelId + "\"," + "\"title\": \""
				+ title + "\"," + "\"description\": \"" + caption + "\"" + "}}";

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add("snippet", new ByteArrayResource(snippetJson.getBytes(StandardCharsets.UTF_8)) {
			@Override
			public String getFilename() {
				return "snippet.json";
			}
		});
		body.add("file", byteArrayResourceFactory.createByteArrayResource(mediaFile.getBytes(),
				mediaFile.getOriginalFilename()));

		HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
		ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.POST, requestEntity,
				String.class);

		if (response.getStatusCode() == HttpStatus.OK) {
			System.out.println("Video sent successfully!");
		} else {
			System.err.println("Failed to send video. Status code: " + response.getStatusCode());
			System.err.println("Response body: " + response.getBody());
		}
	}

}