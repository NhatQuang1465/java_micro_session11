package com.example.demoredisspringboot.controller;

import com.example.demoredisspringboot.model.entity.Product;
import com.example.demoredisspringboot.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    @GetMapping
    public ResponseEntity<?> getProducts(){
        List<Product> productList = productService.getProducts();
        return ResponseEntity.ok(productList);
    }
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product){
        Product productNew = productService.createProduct(product);
        return new ResponseEntity<>(productNew, HttpStatus.CREATED);
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id){
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@RequestBody Product product){
        Product productUpdate = productService.updateProduct(product);
        return ResponseEntity.ok(product);
    }
    @PostMapping("/{id}/by")
    public String byProduct(@PathVariable Long id,@RequestParam(defaultValue = "1") int qty){
        return productService.byProduct(id,qty);
    }

}
