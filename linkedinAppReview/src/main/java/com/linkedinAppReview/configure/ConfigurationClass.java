package com.linkedinAppReview.configure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.linkedinAppReview.dto.FacebookPageDetails;
import com.linkedinAppReview.response.ErrorResponse;
import com.linkedinAppReview.response.ResponseStructure;
import com.linkedinAppReview.response.ResponseWrapper;
import com.linkedinAppReview.response.SuccessResponse;
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Version;

@Component
public class ConfigurationClass {
	
	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public HttpHeaders httpHeaders() {
        return new HttpHeaders();
    }

	@Bean
	@Lazy
	public HttpEntity<String> getHttpEntity(String jsonString, HttpHeaders headers) {
		return new HttpEntity<>(jsonString, headers);
	}

	@Bean
	@Lazy
	public HttpEntity<String> getHttpEntity(HttpHeaders headers) {
		return new HttpEntity<>(headers);
	}

	@Bean
	public Map<String, Object> getMap() {
		return new HashMap<String, Object>();
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	public FacebookPageDetails pageDetails() {
		return new FacebookPageDetails();
	}

	@Bean
	@Lazy
	public FacebookClient getFacebookClient(String accessToken) {
		return new DefaultFacebookClient(accessToken, Version.LATEST);
	}

	@Bean
	public ObjectMetadata getMetaObject() {
		return new ObjectMetadata();
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(ResponseStructure<String> structure) {
		return new ResponseWrapper(structure);
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(SuccessResponse successResponse) {
		return new ResponseWrapper(successResponse);
	}

	@Bean
	@Lazy
	public ResponseWrapper getResponseWrapper(ErrorResponse errorResponse) {
		return new ResponseWrapper(errorResponse);
	}

	@Bean
	public List<Object> getList() {
		return new ArrayList<Object>();
	}

	@Bean
	public SuccessResponse getSuccessResponse() {
		return new SuccessResponse();
	}

	@Bean
	public ErrorResponse getErrorResponse() {
		return new ErrorResponse();
	}
	
	@Bean
	@Lazy
	public HttpEntity<MultiValueMap<String, Object>> getHttpEntityWithMap(MultiValueMap<String, Object> multiValueMap,
			HttpHeaders headers) {
		return new HttpEntity<>(multiValueMap, headers);
	}
	
	@Bean
    public ByteArrayResourceFactory byteArrayResourceFactory() {
        return new ByteArrayResourceFactory();
    }

    public static class ByteArrayResourceFactory {
        public ByteArrayResource createByteArrayResource(byte[] byteArray, String filename) {
            return new ByteArrayResource(byteArray) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };
        }
    }

}