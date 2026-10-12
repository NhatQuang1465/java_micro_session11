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
public class PharmacyAlertMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String type;
    private String message;
}
