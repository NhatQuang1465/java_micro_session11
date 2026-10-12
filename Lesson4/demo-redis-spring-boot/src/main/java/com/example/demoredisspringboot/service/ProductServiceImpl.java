package com.example.demoredisspringboot.service;

import com.example.demoredisspringboot.model.entity.Product;
import com.example.demoredisspringboot.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService{
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;
    @Override
    @Cacheable(value = "products",key = "'all'")
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @Override
    @CacheEvict(value = "products",allEntries = true)
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @Cacheable(value = "products",key = "#id")
    public Product getProductById(Long id) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return productRepository.findById(id).orElseThrow(()->new RuntimeException("KO tin  thay"));
    }

    @Override
    @CachePut(value = "products",key = "#product.id")
    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    @Override
    @CacheEvict(value = "products",key = "#id")
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Override
    public String byProduct(Long id, int qty) {
        // 1 tao lock key rieng theo id san pham
        String lockKey = "lock:product:"+id;
        RLock lock = redissonClient.getLock(lockKey);

        /* 2 thu lay lock
        - cho 3s: thoi gian toi da luong ngoi cho de lay lock
        - nha lock 10s: toi gian tu dong nha lcok neu ung ung bi crash trang DeadLock
         */
        try {
            boolean isAcquired = lock.tryLock(3,10, TimeUnit.SECONDS);
            if (!isAcquired){
                return "he thong dang qua tai viui long thu lai sau";

            }
            System.out.println("Da lay lock thanh cong cho san pham "+id);
            return processOrder(id,qty);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isHeldByCurrentThread()){
                lock.unlock();
                System.out.println("Dda gia phong lok cho  product"+id);
            }
        }

    }
    @Transactional
    public String processOrder(Long id,int qty){
        Product product = productRepository.findById(id).orElseThrow(()->new RuntimeException("Ko thay san pham"));
        if (product.getStock() < qty){
            System.err.println(" san pham ko du ");
            return "San pham "+id+"da het roi";
        }
        product.setStock(product.getStock() - qty);
        productRepository.save(product);
        System.out.println("Dat hanh thang cong");
        return "Dat hang thanh cong "+id;
    }
}
