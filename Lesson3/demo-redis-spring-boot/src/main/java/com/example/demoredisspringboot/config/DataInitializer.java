package com.example.demoredisspringboot.config;

import com.example.demoredisspringboot.model.entity.Medicine;
import com.example.demoredisspringboot.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final MedicineRepository medicineRepository;

    @Override
    public void run(String... args) {
        if (medicineRepository.count() == 0) {
            log.info("===> Khởi tạo danh mục thuốc mẫu vào Database...");
            List<Medicine> medicines = List.of(
                    Medicine.builder()
                            .name("Paracetamol 500mg")
                            .category("Giảm đau, hạ sốt")
                            .price(new BigDecimal("25000"))
                            .dosage("500mg - 1 viên/lần khi sốt")
                            .manufacturer("Dược Hậu Giang (DHG)")
                            .stock(1000)
                            .description("Thuốc giảm đau nhanh, hạ sốt cho người lớn và trẻ em")
                            .build(),
                    Medicine.builder()
                            .name("Panadol")
                            .category("Giảm đau, hạ sốt")
                            .price(new BigDecimal("30000"))
                            .dosage("500mg Paracetamol")
                            .manufacturer("GSK")
                            .stock(500)
                            .description("Thuốc giảm đau hạ sốt nhanh")
                            .build(),
                    Medicine.builder()
                            .name("Panadol Extra")
                            .category("Giảm đau nhanh")
                            .price(new BigDecimal("35000"))
                            .dosage("500mg Paracetamol + 65mg Caffeine")
                            .manufacturer("GSK")
                            .stock(500)
                            .description("Giảm các cơn đau cứng đầu như đau đầu, đau cơ")
                            .build(),
                    Medicine.builder()
                            .name("Amoxicillin 500mg")
                            .category("Kháng sinh")
                            .price(new BigDecimal("45000"))
                            .dosage("500mg - uống theo chỉ định bác sĩ")
                            .manufacturer("Mekophar")
                            .stock(300)
                            .description("Kháng sinh điều trị nhiễm khuẩn đường hô hấp, tai mũi họng")
                            .build(),
                    Medicine.builder()
                            .name("Berberin 50mg")
                            .category("Tiêu hóa")
                            .price(new BigDecimal("15000"))
                            .dosage("50mg - 4-6 viên/ngày chia 2 lần")
                            .manufacturer("OPC")
                            .stock(800)
                            .description("Đặc trị tiêu chảy, kiết lỵ, viêm ruột")
                            .build()
            );

            medicineRepository.saveAll(medicines);
            log.info("===> Đã tạo xong 4 mẫu thuốc ban đầu.");
        }
    }
}
