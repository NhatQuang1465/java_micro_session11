package com.example.demoredisspringboot.service;

import com.example.demoredisspringboot.model.entity.Product;

import java.util.List;

public interface ProductService {
    List<Product> getProducts();
    Product createProduct(Product product);
    Product getProductById(Long id);
    Product updateProduct(Product product);
    void  deleteProduct(Long id);
    String byProduct(Long id,int qty);
}
