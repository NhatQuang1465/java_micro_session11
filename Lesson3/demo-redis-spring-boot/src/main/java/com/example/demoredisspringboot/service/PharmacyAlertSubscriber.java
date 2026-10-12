package com.example.demoredisspringboot.service;

import com.example.demoredisspringboot.model.dto.PharmacyAlertMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyAlertSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;

    /**
     * Lắng nghe và nhận message từ Redis Channel "pharmacy-alerts"
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("===> [REDIS CONSUMER] Nhận message từ channel '{}'", channel);

        try {
            // Sử dụng ObjectMapper để chuyển đổi JSON chuỗi sang đối tượng PharmacyAlertMessage
            PharmacyAlertMessage alert = objectMapper.readValue(body, PharmacyAlertMessage.class);

            // In nội dung trực tiếp ra Console theo yêu cầu
            System.out.println("======================================================================");
            System.out.println("🔔 [DASHBOARD NOTIFICATION - CONSOLE]");
            System.out.println("   ➤ Kênh (Channel) : " + channel);
            System.out.println("   ➤ Loại cảnh báo  : " + alert.getType());
            System.out.println("   ➤ Nội dung       : " + alert.getMessage());
            System.out.println("   ➤ JSON Payload   : " + body);
            System.out.println("======================================================================");
        } catch (Exception e) {
            log.warn("Không thể parse JSON sang PharmacyAlertMessage: {}", body, e);
            System.out.println("======================================================================");
            System.out.println("🔔 [DASHBOARD NOTIFICATION - RAW CONSOLE]: " + body);
            System.out.println("======================================================================");
        }
    }
}
