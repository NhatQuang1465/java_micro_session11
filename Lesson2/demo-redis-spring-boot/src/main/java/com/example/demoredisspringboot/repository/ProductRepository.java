package com.example.demoredisspringboot.repository;

import com.example.demoredisspringboot.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product,Long> {
}
