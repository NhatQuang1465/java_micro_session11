package com.example.demoredisspringboot.service;

import com.example.demoredisspringboot.model.dto.ImportMedicineRequest;
import com.example.demoredisspringboot.model.entity.Medicine;

import java.util.List;

public interface MedicineService {
    Medicine getMedicineById(Long id);
    Medicine createMedicine(Medicine medicine);
    Medicine updateMedicine(Long id, Medicine medicine);
    void deleteMedicine(Long id);
    List<Medicine> getAllMedicines();
    Medicine importMedicine(Long id, Integer quantity);
    Medicine importMedicine(ImportMedicineRequest request);
}
