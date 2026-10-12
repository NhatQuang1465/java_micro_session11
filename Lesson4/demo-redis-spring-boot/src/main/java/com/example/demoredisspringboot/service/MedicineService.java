package com.example.demoredisspringboot.service;

import com.example.demoredisspringboot.model.dto.ConcurrentSellResult;
import com.example.demoredisspringboot.model.dto.ImportMedicineRequest;
import com.example.demoredisspringboot.model.dto.SellMedicineRequest;
import com.example.demoredisspringboot.model.dto.SellMedicineResponse;
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

    // Redisson Distributed Lock Methods
    SellMedicineResponse sellMedicine(Long id, Integer quantity, String employeeName);
    SellMedicineResponse sellMedicine(SellMedicineRequest request);
    ConcurrentSellResult simulateConcurrentSelling(Long id);
    Medicine resetStock(Long id, Integer stock);
}
