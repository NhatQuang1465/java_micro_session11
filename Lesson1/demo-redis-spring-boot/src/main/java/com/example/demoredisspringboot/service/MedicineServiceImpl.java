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
    @CachePut(value = "medicines", key = "#id")
    public Medicine updateMedicine(Long id, Medicine medicine) {
        log.info("Cập nhật thông tin thuốc ID: {}", id);
        Medicine existingMedicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

        existingMedicine.setName(medicine.getName());
        existingMedicine.setCategory(medicine.getCategory());
        existingMedicine.setPrice(medicine.getPrice());
        existingMedicine.setDosage(medicine.getDosage());
        existingMedicine.setManufacturer(medicine.getManufacturer());
        existingMedicine.setStock(medicine.getStock());
        existingMedicine.setDescription(medicine.getDescription());

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
