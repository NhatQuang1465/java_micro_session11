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
public class ConcurrentSellResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String scenario;
    private Long medicineId;
    private String medicineName;
    private Integer initialStock;
    private Integer finalStock;
    private SellMedicineResponse employee1Result;
    private SellMedicineResponse employee2Result;
    private String conclusion;
}
