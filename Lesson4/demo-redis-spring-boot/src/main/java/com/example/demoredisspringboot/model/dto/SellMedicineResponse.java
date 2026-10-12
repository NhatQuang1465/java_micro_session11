package com.example.demoredisspringboot.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellMedicineResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String employeeName;
    private Long medicineId;
    private String medicineName;
    private Integer requestedQuantity;
    private Integer remainingStock;
    private String message;
    private long executionTimeMs;
}
