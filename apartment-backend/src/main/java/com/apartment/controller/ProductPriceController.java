package com.apartment.controller;

import com.apartment.entity.ProductPrice;
import com.apartment.repository.ProductPriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/product-prices")
public class ProductPriceController {
    @Autowired
    private ProductPriceRepository productPriceRepository;

    @GetMapping("/all")
    public List<ProductPrice> getAll() {
        return productPriceRepository.findAll();
    }

    @PostMapping
    public ProductPrice save(@RequestBody ProductPrice productPrice) {
        return productPriceRepository.save(productPrice);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        productPriceRepository.deleteById(id);
    }
}
