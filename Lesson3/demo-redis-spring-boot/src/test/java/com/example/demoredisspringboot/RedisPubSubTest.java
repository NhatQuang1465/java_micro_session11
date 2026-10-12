package com.example.demoredisspringboot;

import com.example.demoredisspringboot.model.dto.PharmacyAlertMessage;
import com.example.demoredisspringboot.service.PharmacyAlertPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class RedisPubSubTest {

    @Autowired
    private PharmacyAlertPublisher publisher;

    @Autowired
    private com.example.demoredisspringboot.service.MedicineService medicineService;

    @Autowired
    private com.example.demoredisspringboot.repository.MedicineRepository medicineRepository;

    @Test
    void testPublishPharmacyAlert() {
        assertDoesNotThrow(() -> {
            PharmacyAlertMessage alert = PharmacyAlertMessage.builder()
                    .type("IMPORT")
                    .message("Đã nhập 100 hộp Panadol")
                    .build();
            publisher.publishAlert(alert);
            
            // Chờ một chút để subscriber nhận message từ Redis
            Thread.sleep(1000);
        });
    }

    @Test
    void testImportMedicineTriggerAlert() {
        assertDoesNotThrow(() -> {
            // Lấy thuốc Panadol từ repository hoặc tạo mới
            var panadolOpt = medicineRepository.findAll().stream()
                    .filter(m -> m.getName().contains("Panadol"))
                    .findFirst();

            Long medicineId = panadolOpt.map(com.example.demoredisspringboot.model.entity.Medicine::getId).orElse(1L);

            // Thực hiện nhập 100 hộp
            medicineService.importMedicine(medicineId, 100);

            // Đợi subscriber nhận và in ra console
            Thread.sleep(1000);
        });
    }
}
