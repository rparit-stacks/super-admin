package com.sara.superadmin.ai;

import com.sara.superadmin.model.AiProviderConfig;
import com.sara.superadmin.service.AiProviderConfigService;
import com.sara.superadmin.web.ApiException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Google Gemini provider. Uses the generateContent REST endpoint with the API key as a
 * query param. No SDK — plain JSON over {@code aiRestTemplate}.
 */
@Component
public class GeminiProvider implements AiProvider {

	private static final Logger log = LoggerFactory.getLogger(GeminiProvider.class);
	private static final String BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

	private final AiProviderConfigService configService;
	private final RestTemplate restTemplate;

	public GeminiProvider(AiProviderConfigService configService, RestTemplate aiRestTemplate) {
		this.configService = configService;
		this.restTemplate = aiRestTemplate;
	}

	@Override
	public String code() {
		return "GEMINI";
	}

	@Override
	public boolean isEnabled() {
		AiProviderConfig c = configService.getOrEmpty();
		return c.isGeminiEnabled() && c.getGeminiApiKey() != null && !c.getGeminiApiKey().isBlank();
	}

	@Override
	public AiAnalysisResult analyze(AiAnalysisPrompt prompt) {
		AiProviderConfig c = configService.getOrEmpty();
		if (c.getGeminiApiKey() == null || c.getGeminiApiKey().isBlank()) {
			throw ApiException.server("Gemini API key is not configured");
		}
		String model = (c.getGeminiModel() == null || c.getGeminiModel().isBlank())
				? "gemini-2.0-flash" : c.getGeminiModel();

		long start = System.nanoTime();
		try {
			JSONObject body = new JSONObject();
			// Combine system instruction + user content into one user turn (simplest, robust).
			JSONArray contents = new JSONArray();
			JSONObject userTurn = new JSONObject();
			userTurn.put("role", "user");
			JSONArray parts = new JSONArray();
			parts.put(new JSONObject().put("text",
					prompt.systemInstruction() + "\n\n" + prompt.userContent()));
			userTurn.put("parts", parts);
			contents.put(userTurn);
			body.put("contents", contents);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			String url = BASE + model + ":generateContent?key=" + c.getGeminiApiKey();
			ResponseEntity<String> resp = restTemplate.exchange(
					url, HttpMethod.POST, new HttpEntity<>(body.toString(), headers), String.class);

			long latency = (System.nanoTime() - start) / 1_000_000;
			String text = extractText(resp.getBody());
			return AiResponseParser.toResult(text, resp.getBody(), model, latency);
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			log.error("Gemini analysis failed", e);
			throw ApiException.server("Gemini analysis failed: " + e.getMessage());
		}
	}

	/** Pull the first candidate's concatenated text parts. */
	private static String extractText(String responseBody) {
		if (responseBody == null || responseBody.isBlank()) {
			return "";
		}
		JSONObject json = new JSONObject(responseBody);
		JSONArray candidates = json.optJSONArray("candidates");
		if (candidates == null || candidates.length() == 0) {
			return "";
		}
		JSONObject content = candidates.getJSONObject(0).optJSONObject("content");
		if (content == null) {
			return "";
		}
		JSONArray parts = content.optJSONArray("parts");
		if (parts == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parts.length(); i++) {
			sb.append(parts.getJSONObject(i).optString("text", ""));
		}
		return sb.toString();
	}
}
