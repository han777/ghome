package com.apartment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for WeChat Work (企业微信) API integration.
 * Handles OAuth2 code exchange, access token management, and user info retrieval.
 */
@Service
public class WeComService {

    @Value("${wecom.corpid}")
    private String corpId;

    @Value("${wecom.secret}")
    private String secret;

    @Value("${wecom.agentid}")
    private String agentId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeComService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Cache access token with expiration tracking
    private String cachedAccessToken;
    private long tokenExpireTime;

    /**
     * Get access token from WeChat Work API, with caching.
     * Token is cached for its validity period (typically 7200 seconds).
     */
    public String getAccessToken() {
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpireTime) {
            return cachedAccessToken;
        }

        String url = String.format(
            "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s",
            corpId, secret
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            if (node.get("errcode").asInt() != 0) {
                throw new RuntimeException("WeChat Work API error: " + node.get("errmsg").asText());
            }

            cachedAccessToken = node.get("access_token").asText();
            int expiresIn = node.get("expires_in").asInt();
            // Cache for 200 seconds less than actual expiry to be safe
            tokenExpireTime = System.currentTimeMillis() + (expiresIn - 200) * 1000L;

            return cachedAccessToken;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get WeChat Work access token: " + e.getMessage(), e);
        }
    }

    /**
     * Get user ID from WeChat Work using the OAuth2 code.
     * This is the first step after receiving the callback code.
     */
    public String getUserIdByCode(String code) {
        String accessToken = getAccessToken();
        String url = String.format(
            "https://qyapi.weixin.qq.com/cgi-bin/auth/getuserinfo?access_token=%s&code=%s",
            accessToken, code
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            if (node.get("errcode").asInt() != 0) {
                throw new RuntimeException("WeChat Work getUserId error: " + node.get("errmsg").asText());
            }

            // The response contains "userid" (企业微信内部用户ID)
            return node.get("userid").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get WeChat Work user ID: " + e.getMessage(), e);
        }
    }

    /**
     * Get detailed user info from WeChat Work by user ID.
     * Returns user details including name, mobile, email etc.
     */
    public WeComUserInfo getUserInfo(String userId) {
        String accessToken = getAccessToken();
        String url = String.format(
            "https://qyapi.weixin.qq.com/cgi-bin/user/get?access_token=%s&userid=%s",
            accessToken, userId
        );

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode node = objectMapper.readTree(response);

            if (node.get("errcode").asInt() != 0) {
                throw new RuntimeException("WeChat Work getUserInfo error: " + node.get("errmsg").asText());
            }

            WeComUserInfo userInfo = new WeComUserInfo();
            userInfo.userId = node.has("userid") ? node.get("userid").asText() : null;
            userInfo.name = node.has("name") ? node.get("name").asText() : null;
            userInfo.mobile = node.has("mobile") ? node.get("mobile").asText() : null;
            userInfo.email = node.has("email") ? node.get("email").asText() : null;
            userInfo.position = node.has("position") ? node.get("position").asText() : null;
            userInfo.avatar = node.has("avatar") ? node.get("avatar").asText() : null;

            return userInfo;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get WeChat Work user info: " + e.getMessage(), e);
        }
    }

    /**
     * Construct the OAuth2 authorization URL for WeChat Work.
     * This URL is used to redirect users to the WeChat Work login page.
     */
    public String buildOAuthUrl(String redirectUri, String state) {
        return String.format(
            "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_privateinfo&state=%s&agentid=%s#wechat_redirect",
            corpId,
            java.net.URLEncoder.encode(redirectUri, java.nio.charset.StandardCharsets.UTF_8),
            state,
            agentId
        );
    }

    /**
     * Inner class to hold WeChat Work user info.
     */
    public static class WeComUserInfo {
        public String userId;
        public String name;
        public String mobile;
        public String email;
        public String position;
        public String avatar;
    }

    /**
     * Send a text message to a WeChat Work user via /cgi-bin/message/send.
     * @param toUser The WeChat Work user ID (userid)
     * @param content The message content (text)
     * @return true if sent successfully, false otherwise
     */
    public boolean sendTextMessage(String toUser, String content) {
        String accessToken = getAccessToken();
        String url = String.format(
            "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s",
            accessToken
        );

        try {
            String body = String.format(
                "{\"touser\":\"%s\",\"msgtype\":\"text\",\"agentid\":%s,\"text\":{\"content\":\"%s\"}}",
                toUser, agentId, escapeJson(content)
            );

            String response = restTemplate.postForObject(url, body, String.class);
            JsonNode node = objectMapper.readTree(response);

            int errcode = node.get("errcode").asInt();
            if (errcode == 0) {
                return true;
            } else {
                throw new RuntimeException("errcode=" + errcode + ", errmsg=" + node.get("errmsg").asText());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to send WeChat Work message: " + e.getMessage(), e);
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}