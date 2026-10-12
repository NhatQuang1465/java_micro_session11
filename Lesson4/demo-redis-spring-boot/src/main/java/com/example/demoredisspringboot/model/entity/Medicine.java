package com.example.demoredisspringboot.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "medicines")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    private BigDecimal price;

    private String dosage; // Liều dùng / hàm lượng (e.g. 500mg)

    private String manufacturer; // Nhà sản xuất

    private Integer stock;

    private String description;
}
