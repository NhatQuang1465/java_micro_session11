package com.example.demoredisspringboot.service;

import com.example.demoredisspringboot.model.dto.ImportMedicineRequest;
import com.example.demoredisspringboot.model.dto.PharmacyAlertMessage;
import com.example.demoredisspringboot.model.entity.Medicine;
import com.example.demoredisspringboot.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final PharmacyAlertPublisher pharmacyAlertPublisher;

    /**
     * Cache-Aside Pattern:
     * 1. Spring Boot checks Redis cache with key "medicines::<id>".
     * 2. Cache HIT: returns the cached object directly without running this method (no SQL).
     * 3. Cache MISS: executes this method (runs SQL query on Database), stores result in Redis, then returns it.
     */
    @Override
    @Cacheable(value = "medicines", key = "#id")
    public Medicine getMedicineById(Long id) {
        log.info("===> [CACHE MISS] Truy vấn Database tìm thuốc với ID: {}", id);
        
        // Mô phỏng độ trễ truy vấn Database (ví dụ hàng nghìn bản ghi hoặc mạng)
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));
    }

    @Override
    @CacheEvict(value = "medicines", allEntries = true)
    public Medicine createMedicine(Medicine medicine) {
        log.info("Thêm mới thuốc: {}", medicine.getName());
        Medicine saved = medicineRepository.save(medicine);

        // Phát thông báo nếu thuốc mới có số lượng nhập ban đầu
        if (saved.getStock() != null && saved.getStock() > 0) {
            String alertMessage = "Đã nhập " + saved.getStock() + " hộp " + saved.getName();
            pharmacyAlertPublisher.publishAlert("IMPORT", alertMessage);
        }

        return saved;
    }

    @Override
    @CacheEvict(value = "medicines", key = "#medicine.id")
    public Medicine updateMedicine(Long id, Medicine medicine) {
        log.info("===> [CACHE EVICT] Cập nhật thông tin thuốc ID: {} và xóa cache key 'medicines::{}'", id, id);
        medicine.setId(id);
        Medicine existingMedicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

        if (medicine.getName() != null) {
            existingMedicine.setName(medicine.getName());
        }
        if (medicine.getCategory() != null) {
            existingMedicine.setCategory(medicine.getCategory());
        }
        if (medicine.getPrice() != null) {
            existingMedicine.setPrice(medicine.getPrice());
        }
        if (medicine.getDosage() != null) {
            existingMedicine.setDosage(medicine.getDosage());
        }
        if (medicine.getManufacturer() != null) {
            existingMedicine.setManufacturer(medicine.getManufacturer());
        }
        if (medicine.getStock() != null) {
            existingMedicine.setStock(medicine.getStock());
        }
        if (medicine.getDescription() != null) {
            existingMedicine.setDescription(medicine.getDescription());
        }

        return medicineRepository.save(existingMedicine);
    }

    @Override
    @CacheEvict(value = "medicines", key = "#id")
    public void deleteMedicine(Long id) {
        log.info("Xóa thuốc và xóa cache ID: {}", id);
        medicineRepository.deleteById(id);
    }

    @Override
    public List<Medicine> getAllMedicines() {
        return medicineRepository.findAll();
    }

    /**
     * Nhập hàng thuốc theo ID: Cập nhật tồn kho và bắn tin nhắn cảnh báo lên Redis Pub/Sub
     */
    @Override
    @CacheEvict(value = "medicines", key = "#id")
    public Medicine importMedicine(Long id, Integer quantity) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

        int qty = (quantity != null && quantity > 0) ? quantity : 100;
        int currentStock = medicine.getStock() != null ? medicine.getStock() : 0;
        medicine.setStock(currentStock + qty);

        Medicine saved = medicineRepository.save(medicine);
        log.info("===> [NHẬP HÀNG] Đã nhập {} hộp cho thuốc '{}' (ID: {}). Tồn kho mới: {}", 
                qty, saved.getName(), id, saved.getStock());

        // Publisher: Gửi alert JSON tới channel "pharmacy-alerts"
        // Định dạng: { "type": "IMPORT", "message": "Đã nhập 100 hộp Panadol" }
        String alertText = "Đã nhập " + qty + " hộp " + saved.getName();
        pharmacyAlertPublisher.publishAlert("IMPORT", alertText);

        return saved;
    }

    /**
     * Nhập hàng thuốc theo request (hỗ trợ nhập theo ID hoặc theo tên)
     */
    @Override
    public Medicine importMedicine(ImportMedicineRequest request) {
        if (request.getMedicineId() != null) {
            return importMedicine(request.getMedicineId(), request.getQuantity());
        }

        int qty = (request.getQuantity() != null && request.getQuantity() > 0) ? request.getQuantity() : 100;
        String name = (request.getMedicineName() != null && !request.getMedicineName().isBlank())
                ? request.getMedicineName() : "Panadol";

        // Tìm thuốc theo tên trong DB, nếu không có tạo mới
        Medicine medicine = medicineRepository.findAll().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(() -> Medicine.builder()
                        .name(name)
                        .category("Thuốc giảm đau")
                        .stock(0)
                        .build());

        return importMedicine(medicine.getId() != null ? medicine.getId() : medicineRepository.save(medicine).getId(), qty);
    }
}
