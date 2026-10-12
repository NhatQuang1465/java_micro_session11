package com.example.demoredisspringboot.controller;

import com.example.demoredisspringboot.model.dto.ConcurrentSellResult;
import com.example.demoredisspringboot.model.dto.ImportMedicineRequest;
import com.example.demoredisspringboot.model.dto.PharmacyAlertMessage;
import com.example.demoredisspringboot.model.dto.SellMedicineRequest;
import com.example.demoredisspringboot.model.dto.SellMedicineResponse;
import com.example.demoredisspringboot.model.entity.Medicine;
import com.example.demoredisspringboot.service.MedicineService;
import com.example.demoredisspringboot.service.PharmacyAlertPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    private final PharmacyAlertPublisher pharmacyAlertPublisher;

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMedicineById(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();
        Medicine medicine = medicineService.getMedicineById(id);
        long responseTimeMs = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("data", medicine);
        response.put("responseTimeMs", responseTimeMs);
        response.put("message", responseTimeMs < 15 ? "Cache HIT (từ Redis)" : "Cache MISS (từ Database)");

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Medicine> createMedicine(@RequestBody Medicine medicine) {
        Medicine created = medicineService.createMedicine(medicine);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Medicine> updateMedicine(@PathVariable Long id, @RequestBody Medicine medicine) {
        medicine.setId(id);
        Medicine updated = medicineService.updateMedicine(id, medicine);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMedicine(@PathVariable Long id) {
        medicineService.deleteMedicine(id);
        return ResponseEntity.ok("Xóa thành công thuốc ID: " + id);
    }

    @GetMapping
    public ResponseEntity<List<Medicine>> getAllMedicines() {
        return ResponseEntity.ok(medicineService.getAllMedicines());
    }

    /**
     * API Nhập hàng theo ID thuốc:
     * POST /api/v1/medicines/{id}/import?quantity=100
     * Sau khi nhập thành công sẽ gửi thông báo vào Redis channel "pharmacy-alerts"
     */
    @PostMapping("/{id}/import")
    public ResponseEntity<Map<String, Object>> importMedicineById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "100") Integer quantity) {
        Medicine updatedMedicine = medicineService.importMedicine(id, quantity);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Nhập hàng thành công! Đã phát thông báo tới Redis Channel 'pharmacy-alerts'.");
        response.put("medicine", updatedMedicine);
        response.put("importedQuantity", quantity);

        return ResponseEntity.ok(response);
    }

    /**
     * API Nhập hàng bằng Request Body:
     * POST /api/v1/medicines/import
     * Body: { "medicineId": 2, "quantity": 100 } hoặc { "medicineName": "Panadol", "quantity": 100 }
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importMedicine(
            @RequestBody ImportMedicineRequest request) {
        Medicine updatedMedicine = medicineService.importMedicine(request);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Nhập hàng thành công! Đã phát thông báo tới Redis Channel 'pharmacy-alerts'.");
        response.put("medicine", updatedMedicine);
        response.put("importedQuantity", request.getQuantity());

        return ResponseEntity.ok(response);
    }

    /**
     * API Gửi trực tiếp tin nhắn cảnh báo vào Redis Channel pharmacy-alerts:
     * POST /api/v1/medicines/alerts/publish
     * Body: { "type": "IMPORT", "message": "Đã nhập 100 hộp Panadol" }
     */
    @PostMapping("/alerts/publish")
    public ResponseEntity<Map<String, Object>> publishAlert(
            @RequestBody PharmacyAlertMessage alertMessage) {
        pharmacyAlertPublisher.publishAlert(alertMessage);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("sentAlert", alertMessage);
        response.put("note", "Tin nhắn đã được gửi qua Redis Pub/Sub (Fire-and-forget)");

        return ResponseEntity.ok(response);
    }

    /**
     * API Bán thuốc theo ID (Hỗ trợ query params):
     * POST /api/v1/medicines/{id}/sell?quantity=1&employeeName=Nhân viên A
     */
    @PostMapping("/{id}/sell")
    public ResponseEntity<SellMedicineResponse> sellMedicineById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(defaultValue = "Nhân viên") String employeeName) {
        SellMedicineResponse response = medicineService.sellMedicine(id, quantity, employeeName);
        return ResponseEntity.ok(response);
    }

    /**
     * API Bán thuốc theo Request Body:
     * POST /api/v1/medicines/sell
     * Body: { "medicineId": 1, "quantity": 1, "employeeName": "Nhân viên 1" }
     */
    @PostMapping("/sell")
    public ResponseEntity<SellMedicineResponse> sellMedicine(
            @RequestBody SellMedicineRequest request) {
        SellMedicineResponse response = medicineService.sellMedicine(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API MÔ PHỎNG RACE CONDITION (2 LUỒNG CHẠY SONG SONG):
     * POST /api/v1/medicines/{id}/test-race-condition
     * hoặc POST /api/v1/medicines/{id}/sell-concurrent
     * Mô phỏng 2 nhân viên tại 2 máy tính khác nhau cùng nhấn "Thanh toán" tại cùng 1 mili giây.
     * Sử dụng Redisson Distributed Lock để đảm bảo chỉ đúng 1 người mua được, người kia báo hết hàng, không bị âm kho.
     */
    @PostMapping({ "/{id}/test-race-condition", "/{id}/sell-concurrent" })
    public ResponseEntity<ConcurrentSellResult> testRaceCondition(
            @PathVariable Long id) {
        ConcurrentSellResult result = medicineService.simulateConcurrentSelling(id);
        return ResponseEntity.ok(result);
    }

    /**
     * API Reset tồn kho để phục vụ chạy test nhiều lần:
     * POST /api/v1/medicines/{id}/reset-stock?stock=1
     */
    @PostMapping("/{id}/reset-stock")
    public ResponseEntity<Map<String, Object>> resetStock(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer stock) {
        Medicine medicine = medicineService.resetStock(id, stock);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Đã reset tồn kho thành công!");
        response.put("medicineId", medicine.getId());
        response.put("medicineName", medicine.getName());
        response.put("currentStock", medicine.getStock());
        return ResponseEntity.ok(response);
    }
}
