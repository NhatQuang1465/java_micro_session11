package com.example.demoredisspringboot;

import com.example.demoredisspringboot.model.dto.ConcurrentSellResult;
import com.example.demoredisspringboot.model.dto.SellMedicineResponse;
import com.example.demoredisspringboot.model.entity.Medicine;
import com.example.demoredisspringboot.repository.MedicineRepository;
import com.example.demoredisspringboot.service.MedicineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MedicineRaceConditionTest {

    @Autowired
    private MedicineService medicineService;

    @Autowired
    private MedicineRepository medicineRepository;

    private Long rareMedicineId;

    @BeforeEach
    void setUp() {
        // Chuẩn bị một hộp thuốc hiếm chỉ còn đúng 1 sản phẩm cuối cùng
        Medicine rareMedicine = medicineRepository.findAll().stream()
                .filter(m -> m.getName().contains("Thuốc hiếm"))
                .findFirst()
                .orElseGet(() -> medicineRepository.save(
                        Medicine.builder()
                                .name("Huyết thanh kháng độc (Thuốc hiếm)")
                                .category("Thuốc hiếm đặc trị")
                                .price(new BigDecimal("15000000"))
                                .dosage("1 lọ duy nhất")
                                .stock(1)
                                .build()
                ));

        rareMedicine.setStock(1);
        medicineRepository.saveAndFlush(rareMedicine);
        rareMedicineId = rareMedicine.getId();
    }

    @Test
    @DisplayName("Kiểm thử 2 nhân viên cùng nhấn 'Thanh toán' sản phẩm cuối cùng với Redisson Lock")
    void testTwoEmployeesBuyLastMedicineConcurrently() throws Exception {
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Luồng 1: Nhân viên 1 (Máy tính A)
        Future<SellMedicineResponse> future1 = executor.submit(() -> {
            readyLatch.countDown();
            readyLatch.await(); // Đợi luồng 2 cùng sẵn sàng
            startLatch.await(); // Cùng xuất phát tại đúng 1 thời điểm
            return medicineService.sellMedicine(rareMedicineId, 1, "Nhân viên A (Máy 1)");
        });

        // Luồng 2: Nhân viên 2 (Máy tính B)
        Future<SellMedicineResponse> future2 = executor.submit(() -> {
            readyLatch.countDown();
            readyLatch.await(); // Đợi luồng 1 cùng sẵn sàng
            startLatch.await(); // Cùng xuất phát tại đúng 1 thời điểm
            return medicineService.sellMedicine(rareMedicineId, 1, "Nhân viên B (Máy 2)");
        });

        // Bắn tín hiệu bắt đầu đồng thời
        readyLatch.await();
        startLatch.countDown();

        SellMedicineResponse result1 = future1.get(10, TimeUnit.SECONDS);
        SellMedicineResponse result2 = future2.get(10, TimeUnit.SECONDS);

        executor.shutdown();

        // 1. Phải có chính xác 1 người thành công và 1 người thất bại
        int successCount = (result1.isSuccess() ? 1 : 0) + (result2.isSuccess() ? 1 : 0);
        int failureCount = (!result1.isSuccess() ? 1 : 0) + (!result2.isSuccess() ? 1 : 0);

        assertEquals(1, successCount, "Chỉ có đúng 1 nhân viên mua được hộp thuốc cuối cùng!");
        assertEquals(1, failureCount, "Nhân viên còn lại phải bị thông báo hết hàng!");

        // 2. Người thất bại phải nhận được thông báo hết hàng
        SellMedicineResponse failedResult = !result1.isSuccess() ? result1 : result2;
        assertTrue(failedResult.getMessage().contains("HẾT HÀNG"), "Thông báo phải chỉ rõ là hết hàng");

        // 3. Người thành công phải nhận được thông báo thanh toán thành công
        SellMedicineResponse successResult = result1.isSuccess() ? result1 : result2;
        assertTrue(successResult.getMessage().contains("THANH TOÁN THÀNH CÔNG"), "Thông báo phải xác nhận thành công");

        // 4. Tồn kho trong Database phải chính xác là 0 (KHÔNG BỊ ÂM KHO)
        Medicine updated = medicineRepository.findById(rareMedicineId).orElseThrow();
        assertEquals(0, updated.getStock(), "Tồn kho sau khi bán phải là 0, không được âm kho!");
    }

    @Test
    @DisplayName("Kiểm thử phương thức simulateConcurrentSelling tích hợp")
    void testSimulateConcurrentSelling() {
        medicineService.resetStock(rareMedicineId, 1);

        ConcurrentSellResult result = medicineService.simulateConcurrentSelling(rareMedicineId);

        assertNotNull(result);
        assertEquals(1, result.getInitialStock());
        assertEquals(0, result.getFinalStock());

        int successCount = (result.getEmployee1Result().isSuccess() ? 1 : 0) 
                + (result.getEmployee2Result().isSuccess() ? 1 : 0);
        assertEquals(1, successCount, "Chỉ 1 trong 2 luồng thành công");

        int failCount = (!result.getEmployee1Result().isSuccess() ? 1 : 0) 
                + (!result.getEmployee2Result().isSuccess() ? 1 : 0);
        assertEquals(1, failCount, "Luồng còn lại phải báo hết hàng");
    }
}
