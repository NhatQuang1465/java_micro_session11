package com.example.demoredisspringboot.service;

import com.example.demoredisspringboot.model.dto.ConcurrentSellResult;
import com.example.demoredisspringboot.model.dto.ImportMedicineRequest;
import com.example.demoredisspringboot.model.dto.PharmacyAlertMessage;
import com.example.demoredisspringboot.model.dto.SellMedicineRequest;
import com.example.demoredisspringboot.model.dto.SellMedicineResponse;
import com.example.demoredisspringboot.model.entity.Medicine;
import com.example.demoredisspringboot.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final PharmacyAlertPublisher pharmacyAlertPublisher;
    private final RedissonClient redissonClient;

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

    /**
     * Bán thuốc với Redisson Distributed Lock (Chống Race Condition trong hệ thống phân tán)
     * - Key lock: lock:medicine:{id}
     * - tryLock: wait time = 3s, lease time = 5s
     * - Kiểm tra tồn kho và trừ số lượng sau khi đã lấy được lock an toàn.
     */
    @Override
    @CacheEvict(value = "medicines", key = "#id")
    public SellMedicineResponse sellMedicine(Long id, Integer quantity, String employeeName) {
        long startTime = System.currentTimeMillis();
        String emp = (employeeName != null && !employeeName.isBlank()) ? employeeName : "Nhân viên";
        int qty = (quantity != null && quantity > 0) ? quantity : 1;

        String lockKey = "lock:medicine:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        log.info("[{}] Đang cố gắng lấy distributed lock '{}'...", emp, lockKey);

        try {
            // Yêu cầu: tryLock với thời gian chờ (wait time) là 3 giây và thời gian giữ lock (lease time) là 5 giây
            isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);

            if (!isLocked) {
                log.warn("[{}] THẤT BẠI: Không lấy được lock sau 3 giây chờ đợi (Hệ thống đang bận).", emp);
                return SellMedicineResponse.builder()
                        .success(false)
                        .employeeName(emp)
                        .medicineId(id)
                        .requestedQuantity(qty)
                        .remainingStock(null)
                        .message("Hệ thống đang bận hoặc có giao dịch khác đang xử lý. Vui lòng thử lại sau!")
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            log.info("[{}] >>> ĐÃ LẤY ĐƯỢC LOCK '{}'. Bắt đầu kiểm tra tồn kho và thanh toán...", emp, lockKey);

            Medicine medicine = medicineRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

            int currentStock = medicine.getStock() != null ? medicine.getStock() : 0;
            log.info("[{}] Tồn kho hiện tại của '{}' (ID: {}): {}", emp, medicine.getName(), id, currentStock);

            // Kiểm tra tồn kho trước khi trừ số lượng
            if (currentStock < qty) {
                log.warn("[{}] THẤT BẠI: HẾT HÀNG! Tồn kho hiện tại ({}) < số lượng mua ({})", emp, currentStock, qty);
                return SellMedicineResponse.builder()
                        .success(false)
                        .employeeName(emp)
                        .medicineId(id)
                        .medicineName(medicine.getName())
                        .requestedQuantity(qty)
                        .remainingStock(currentStock)
                        .message("HẾT HÀNG! Hộp thuốc này chỉ còn " + currentStock + " sản phẩm trong kho, không đủ để bán " + qty + " sản phẩm.")
                        .executionTimeMs(System.currentTimeMillis() - startTime)
                        .build();
            }

            // Mô phỏng độ trễ xử lý thanh toán (150ms) để làm nổi bật tác dụng của Lock khi có nhiều luồng cùng gọi
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Trừ số lượng tồn kho và cập nhật Database ngay lập tức
            int newStock = currentStock - qty;
            medicine.setStock(newStock);
            medicineRepository.saveAndFlush(medicine);

            log.info("[{}] <<< THANH TOÁN THÀNH CÔNG! Đã bán {} hộp thuốc '{}'. Tồn kho còn lại: {}", 
                    emp, qty, medicine.getName(), newStock);

            return SellMedicineResponse.builder()
                    .success(true)
                    .employeeName(emp)
                    .medicineId(id)
                    .medicineName(medicine.getName())
                    .requestedQuantity(qty)
                    .remainingStock(newStock)
                    .message("THANH TOÁN THÀNH CÔNG! Bạn đã mua được " + qty + " sản phẩm. Tồn kho còn lại: " + newStock)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[{}] Luồng bị gián đoạn khi đang chờ lấy lock: {}", emp, e.getMessage());
            return SellMedicineResponse.builder()
                    .success(false)
                    .employeeName(emp)
                    .medicineId(id)
                    .requestedQuantity(qty)
                    .message("Quá trình xử lý bị gián đoạn: " + e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("[{}] === ĐÃ GIẢI PHÓNG LOCK '{}' ===", emp, lockKey);
            }
        }
    }

    @Override
    public SellMedicineResponse sellMedicine(SellMedicineRequest request) {
        return sellMedicine(request.getMedicineId(), request.getQuantity(), request.getEmployeeName());
    }

    /**
     * Mô phỏng 2 nhân viên (2 luồng song song) cùng nhấn 'Thanh toán' sản phẩm cuối cùng tại cùng 1 mili giây.
     * Sử dụng CountDownLatch để đảm bảo cả 2 luồng xuất phát đồng thời chính xác.
     */
    @Override
    public ConcurrentSellResult simulateConcurrentSelling(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));

        int initialStock = medicine.getStock() != null ? medicine.getStock() : 0;
        log.info("========== BẮT ĐẦU MÔ PHỎNG 2 LUỒNG BÁN THUỐC SONG SONG (ID: {}, TỒN KHO BAN ĐẦU: {}) ==========", 
                id, initialStock);

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Future<SellMedicineResponse> futureEmp1 = executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await(); // Chờ phát lệnh đồng thời
                return sellMedicine(id, 1, "Nhân viên 1 (Máy tính A)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return SellMedicineResponse.builder()
                        .success(false)
                        .employeeName("Nhân viên 1 (Máy tính A)")
                        .message("Luồng bị ngắt")
                        .build();
            }
        });

        Future<SellMedicineResponse> futureEmp2 = executor.submit(() -> {
            try {
                readyLatch.countDown();
                startLatch.await(); // Chờ phát lệnh đồng thời
                return sellMedicine(id, 1, "Nhân viên 2 (Máy tính B)");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return SellMedicineResponse.builder()
                        .success(false)
                        .employeeName("Nhân viên 2 (Máy tính B)")
                        .message("Luồng bị ngắt")
                        .build();
            }
        });

        try {
            // Chờ cả 2 luồng sẵn sàng
            readyLatch.await();
            log.info(">>> CẢ 2 NHÂN VIÊN ĐÃ SẴN SÀNG. BẮT ĐẦU NHẤN 'THANH TOÁN' CÙNG LÚC! <<<");
            startLatch.countDown(); // Bắn tín hiệu bắt đầu đồng thời

            SellMedicineResponse res1 = futureEmp1.get();
            SellMedicineResponse res2 = futureEmp2.get();

            Medicine finalMedicine = medicineRepository.findById(id).orElse(medicine);
            int finalStock = finalMedicine.getStock() != null ? finalMedicine.getStock() : 0;

            executor.shutdown();

            String successEmployee = res1.isSuccess() ? res1.getEmployeeName() 
                    : (res2.isSuccess() ? res2.getEmployeeName() : "Không ai");
            String failedEmployee = !res1.isSuccess() ? res1.getEmployeeName() 
                    : (!res2.isSuccess() ? res2.getEmployeeName() : "Không ai");

            log.info("========== KẾT THÚC MÔ PHỎNG. TỒN KHO CUỐI CÙNG: {} ==========", finalStock);

            return ConcurrentSellResult.builder()
                    .scenario("Mô phỏng 2 nhân viên ở 2 máy tính khác nhau cùng nhấn 'Thanh toán' tại cùng 1 mili giây")
                    .medicineId(id)
                    .medicineName(finalMedicine.getName())
                    .initialStock(initialStock)
                    .finalStock(finalStock)
                    .employee1Result(res1)
                    .employee2Result(res2)
                    .conclusion("Redisson Distributed Lock đã giải quyết hoàn toàn Race Condition: "
                            + successEmployee + " lấy được Lock và mua thành công; "
                            + failedEmployee + " đợi Lock xong kiểm tra thấy hết hàng và nhận thông báo hết hàng. "
                            + "Tồn kho an toàn (" + finalStock + "), không xảy ra lỗi bán âm kho.")
                    .build();

        } catch (Exception e) {
            executor.shutdownNow();
            throw new RuntimeException("Lỗi khi mô phỏng chạy song song: " + e.getMessage(), e);
        }
    }

    @Override
    @CacheEvict(value = "medicines", key = "#id")
    public Medicine resetStock(Long id, Integer stock) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thuốc với ID: " + id));
        medicine.setStock(stock != null ? stock : 1);
        Medicine saved = medicineRepository.saveAndFlush(medicine);
        log.info("===> Đã reset tồn kho của thuốc '{}' (ID: {}) về {}", saved.getName(), id, saved.getStock());
        return saved;
    }
}
