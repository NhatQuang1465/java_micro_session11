package com.example.demoredisspringboot.service;

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
        return medicineRepository.save(medicine);
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
}
