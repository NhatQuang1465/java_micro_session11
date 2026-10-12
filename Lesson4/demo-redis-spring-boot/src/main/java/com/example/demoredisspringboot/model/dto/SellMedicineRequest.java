package com.example.demoredisspringboot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellMedicineRequest {
    private Long medicineId;
    private Integer quantity;
    private String employeeName;
}
