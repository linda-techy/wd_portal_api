package com.wd.api.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Firebase Cloud Messaging (FCM) push notification sender — Portal API.
 *
 * Design principles:
 *  - Fire-and-forget: exceptions are caught and logged; never propagated.
 *    Push notifications must NEVER break core business operations.
 *  - No-op when Firebase is not initialised (service account file absent).
 *  - sendToTokens uses MulticastMessage (max 500 tokens per call per FCM limit).
 */
@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);
    private static final int FCM_MULTICAST_LIMIT = 500;

    /**
     * Send a push notification to a single device token.
     *
     * @param fcmToken device FCM registration token
     * @param title    notification title
     * @param body     notification body
     * @param data     optional key/value data payload (for deep-link navigation)
     */
    public void sendToToken(String fcmToken, String title, String body, Map<String, String> data) {
        if (!isFirebaseReady() || fcmToken == null || fcmToken.isBlank()) return;

        try {
            Message.Builder builder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }
            String messageId = FirebaseMessaging.getInstance().send(builder.build());
            logger.debug("FCM sent to single token — messageId={}", messageId);
        } catch (FirebaseMessagingException e) {
            logger.warn("FCM send failed for token (portal): {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error sending FCM notification (portal): {}", e.getMessage());
        }
    }

    /**
     * Send a push notification to multiple device tokens (batch).
     * Automatically partitions into chunks of 500 to respect FCM limits.
     *
     * @param tokens list of device FCM registration tokens (nulls/blanks filtered)
     * @param title  notification title
     * @param body   notification body
     * @param data   optional key/value data payload
     */
    public void sendToTokens(List<String> tokens, String title, String body, Map<String, String> data) {
        if (!isFirebaseReady() || tokens == null || tokens.isEmpty()) return;

        List<String> validTokens = tokens.stream()
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        if (validTokens.isEmpty()) return;

        // Partition into chunks of FCM_MULTICAST_LIMIT
        for (int i = 0; i < validTokens.size(); i += FCM_MULTICAST_LIMIT) {
            List<String> chunk = validTokens.subList(i, Math.min(i + FCM_MULTICAST_LIMIT, validTokens.size()));
            sendMulticast(chunk, title, body, data);
        }
    }

    private void sendMulticast(List<String> tokens, String title, String body, Map<String, String> data) {
        try {
            MulticastMessage.Builder builder = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());
            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(builder.build());
            logger.debug("FCM multicast — successCount={} failureCount={}",
                    response.getSuccessCount(), response.getFailureCount());
            if (response.getFailureCount() > 0) {
                response.getResponses().stream()
                        .filter(r -> !r.isSuccessful())
                        .forEach(r -> logger.warn("FCM multicast failure: {}", r.getException().getMessage()));
            }
        } catch (FirebaseMessagingException e) {
            logger.warn("FCM multicast send failed (portal): {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in FCM multicast (portal): {}", e.getMessage());
        }
    }

    private boolean isFirebaseReady() {
        try {
            return !FirebaseApp.getApps().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
