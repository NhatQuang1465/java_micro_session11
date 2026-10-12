package com.example.demoredisspringboot.service;

import com.example.demoredisspringboot.model.dto.PharmacyAlertMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyAlertPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChannelTopic pharmacyAlertTopic;
    private final ObjectMapper objectMapper;

    /**
     * Chuyển đổi đối tượng sang JSON bằng ObjectMapper và gửi vào Redis Channel "pharmacy-alerts"
     */
    public void publishAlert(PharmacyAlertMessage alertMessage) {
        try {
            // Chuyển đối tượng sang chuỗi JSON theo yêu cầu bằng ObjectMapper
            String jsonMessage = objectMapper.writeValueAsString(alertMessage);

            // Gửi message vào channel (Fire-and-forget: Redis Pub/Sub không lưu trữ message)
            stringRedisTemplate.convertAndSend(pharmacyAlertTopic.getTopic(), jsonMessage);

            log.info("===> [REDIS PRODUCER] Đã gửi message tới channel '{}': {}", 
                    pharmacyAlertTopic.getTopic(), jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("❌ Lỗi khi serialize PharmacyAlertMessage sang JSON: {}", e.getMessage(), e);
        }
    }

    /**
     * Tiện ích gửi nhanh alert từ type và message
     */
    public void publishAlert(String type, String message) {
        publishAlert(PharmacyAlertMessage.builder()
                .type(type)
                .message(message)
                .build());
    }
}
