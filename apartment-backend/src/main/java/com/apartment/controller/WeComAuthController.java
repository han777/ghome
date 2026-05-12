package com.apartment.controller;

import com.apartment.entity.SysRole;
import com.apartment.entity.SysUser;
import com.apartment.repository.SysRoleRepository;
import com.apartment.repository.SysUserRepository;
import com.apartment.security.JwtUtils;
import com.apartment.service.WeComCryptoService;
import com.apartment.service.WeComService;
import com.apartment.service.WeComService.WeComUserInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Controller for WeChat Work (企业微信) OAuth2 authentication and callback service.
 *
 * The callback URL supports three modes:
 * 1. GET + msg_signature + echostr → URL verification (when admin configures callback in WeChat Work)
 * 2. GET + code → OAuth2 callback (when user authorizes login)
 * 3. POST + msg_signature → Receive event data (encrypted messages from WeChat Work)
 *
 * Reference: https://developer.work.weixin.qq.com/document/path/90930
 */
@RestController
@RequestMapping("/api/auth/wecom")
public class WeComAuthController {

    @Autowired
    private WeComService weComService;

    @Autowired
    private WeComCryptoService cryptoService;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SysRoleRepository roleRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${wecom.callback.base-url}")
    private String callbackBaseUrl;

    /**
     * Generate WeChat Work OAuth2 authorization URL.
     */
    @GetMapping("/authorize")
    public Map<String, String> authorize(@RequestParam(required = false) String redirect) {
        String callbackUrl = callbackBaseUrl + "/api/auth/wecom/callback";
        String state = redirect != null ? redirect : "/m";
        String oauthUrl = weComService.buildOAuthUrl(callbackUrl, state);
        return Map.of("url", oauthUrl);
    }

    /**
     * GET callback handler - supports two modes:
     *
     * Mode 1: URL Verification (msg_signature + echostr present)
     * - WeChat Work sends this when admin configures the callback URL
     * - Verify signature, decrypt echostr, return plaintext echostr
     *
     * Mode 2: OAuth2 Callback (code present)
     * - User authorizes in WeChat Work, redirected here with code
     * - Exchange code for userId → find/create user → generate JWT → redirect to frontend
     */
    @GetMapping("/callback")
    public void handleGetCallback(
            @RequestParam(required = false) String msg_signature,
            @RequestParam(required = false) String timestamp,
            @RequestParam(required = false) String nonce,
            @RequestParam(required = false) String echostr,
            @RequestParam(required = false) String code,
            @RequestParam(required = false, defaultValue = "/m") String state,
            HttpServletResponse response) throws IOException {

        // Mode 1: URL Verification - WeChat Work callback URL configuration
        if (msg_signature != null && echostr != null && timestamp != null && nonce != null) {
            handleUrlVerification(msg_signature, timestamp, nonce, echostr, response);
            return;
        }

        // Mode 2: OAuth2 Callback - user login authorization
        if (code != null) {
            handleOAuthCallback(code, state, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid request");
    }

    /**
     * POST callback handler - receive encrypted event data from WeChat Work.
     * Currently responds with "success" to acknowledge receipt.
     * Future: parse events (e.g. contact changes, message events) and process accordingly.
     */
    @PostMapping("/callback")
    public String handlePostCallback(
            @RequestParam String msg_signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            HttpServletRequest request) {
        try {
            // Read the XML body containing <Encrypt> field
            String body = request.getReader().lines()
                    .collect(java.util.stream.Collectors.joining());

            // Extract Encrypt field from XML
            String encryptedMsg = extractEncryptFromXml(body);

            if (encryptedMsg == null) {
                return "success";
            }

            // Verify signature
            if (!cryptoService.verifySignature(msg_signature, timestamp, nonce, encryptedMsg)) {
                return "success"; // Invalid signature, still acknowledge
            }

            // Decrypt to get plaintext event XML
            String plaintext = cryptoService.decrypt(encryptedMsg);

            // Log the event for debugging
            System.out.println("WeChat Work callback event received: " + plaintext);

            // Acknowledge receipt - WeChat Work expects "success" or empty response
            return "success";

        } catch (Exception e) {
            System.out.println("WeChat Work POST callback error: " + e.getMessage());
            return "success"; // Always acknowledge to prevent retries
        }
    }

    /**
     * URL Verification: verify signature and return decrypted echostr.
     * This is called when the admin configures the callback URL in WeChat Work admin console.
     */
    private void handleUrlVerification(String msgSignature, String timestamp, String nonce, String echostr, HttpServletResponse response) throws IOException {
        try {
            // Verify the signature
            if (!cryptoService.verifySignature(msgSignature, timestamp, nonce, echostr)) {
                System.out.println("WeChat Work URL verification: signature mismatch");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "signature verification failed");
                return;
            }

            // Decrypt echostr to get plaintext message
            String plaintext = cryptoService.decrypt(echostr);

            System.out.println("WeChat Work URL verification successful, returning: " + plaintext);

            // Return plaintext directly - WeChat Work expects plain text response
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(plaintext);
            response.getWriter().flush();
        } catch (Exception e) {
            System.out.println("WeChat Work URL verification error: " + e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "verification error");
        }
    }

    /**
     * OAuth2 Callback: exchange code for user info, find/create user, generate JWT, redirect.
     */
    private void handleOAuthCallback(String code, String state, HttpServletResponse response) throws IOException {
        try {
            // 1. Use code to get WeChat Work userId
            String wecomUserId = weComService.getUserIdByCode(code);

            // 2. Get detailed user info from WeChat Work
            WeComUserInfo wecomUserInfo = weComService.getUserInfo(wecomUserId);

            // 3. Find existing user by wecomId, or create new one
            SysUser user = userRepository.findByWecomId(wecomUserId).orElse(null);

            if (user == null) {
                // Auto-register: create new user with wecom source
                user = new SysUser();
                user.setWecomId(wecomUserId);
                user.setSource("wecom");
                user.setUsername("wecom_" + wecomUserId);
                user.setPassword(passwordEncoder.encode("wecom_" + System.currentTimeMillis()));
                // Use name from WeChat Work directory as real_name
                user.setRealName(wecomUserInfo.name != null ? wecomUserInfo.name : wecomUserId);
                user.setPhone(wecomUserInfo.mobile);
                user.setEmail(wecomUserInfo.email);
                user.setStatus(1);

                // Assign ROLE_USER by default for wecom users
                SysRole userRole = roleRepository.findByRoleCode("ROLE_USER").orElse(null);
                if (userRole != null) {
                    Set<SysRole> roles = new HashSet<>();
                    roles.add(userRole);
                    user.setRoles(roles);
                }

                user = userRepository.save(user);
                System.out.println("New WeChat Work user registered: " + wecomUserId + " (realName: " + user.getRealName() + ")");
            } else {
                // Always sync real_name from WeChat Work directory to keep consistency
                if (wecomUserInfo.name != null) {
                    user.setRealName(wecomUserInfo.name);
                }
                // Sync phone and email only if they were previously empty
                if (wecomUserInfo.mobile != null && (user.getPhone() == null || user.getPhone().isEmpty())) {
                    user.setPhone(wecomUserInfo.mobile);
                }
                if (wecomUserInfo.email != null && (user.getEmail() == null || user.getEmail().isEmpty())) {
                    user.setEmail(wecomUserInfo.email);
                }
                user = userRepository.save(user);
                System.out.println("WeChat Work user logged in: " + wecomUserId + " (realName updated: " + user.getRealName() + ")");
            }

            // 4. Generate JWT token
            String token = jwtUtils.generateToken(user.getUsername());

            // 5. Redirect to frontend callback page using 302 redirect
            String redirectPath = state != null ? state : "/m";
            String frontendCallbackUrl = String.format(
                "%s/m/auth/callback?token=%s&redirect=%s",
                callbackBaseUrl,
                URLEncoder.encode(token, StandardCharsets.UTF_8),
                URLEncoder.encode(redirectPath, StandardCharsets.UTF_8)
            );

            response.sendRedirect(frontendCallbackUrl);

        } catch (Exception e) {
            System.out.println("WeChat Work OAuth callback error: " + e.getMessage());
            String errorUrl = String.format(
                "%s/m/auth?error=wecom_login_failed&message=%s",
                callbackBaseUrl,
                URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
            );
            response.sendRedirect(errorUrl);
        }
    }

    /**
     * Extract <Encrypt> value from XML body.
     */
    private String extractEncryptFromXml(String xml) {
        if (xml == null || xml.isEmpty()) return null;
        int start = xml.indexOf("<Encrypt><![CDATA[");
        int end = xml.indexOf("]]></Encrypt>");
        if (start < 0 || end < 0) return null;
        start += "<Encrypt><![CDATA[".length();
        return xml.substring(start, end);
    }
}