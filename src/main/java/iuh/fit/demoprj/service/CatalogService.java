package iuh.fit.demoprj.service;

import iuh.fit.demoprj.model.entity.*;
import iuh.fit.demoprj.model.enums.Role;
import iuh.fit.demoprj.repository.*;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogService {
    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    public List<Product> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return products.findAll();
        return products.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    public Product getProduct(Long id) {
        return products.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm."));
    }

    public List<Category> getCategories() {
        return categories.findAll();
    }

    public Category getCategory(Long id) {
        return categories.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục."));
    }

    @Transactional
    public Category saveCategory(Long id, String name) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        boolean duplicate = id == null
                ? categories.existsByNameIgnoreCase(normalizedName)
                : categories.existsByNameIgnoreCaseAndIdNot(normalizedName, id);
        if (duplicate) {
            throw new IllegalArgumentException("Tên danh mục đã tồn tại.");
        }
        Category category = id == null ? new Category(normalizedName) : getCategory(id);
        category.setName(normalizedName);
        return categories.save(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        getCategory(id);
        if (products.existsByCategoryId(id)) {
            throw new IllegalArgumentException("Danh mục đang có sản phẩm, không thể xóa.");
        }
        categories.deleteById(id);
    }

    @Transactional
    public Product saveProduct(Long id, String name, java.math.BigDecimal price, int stock,
                               String description, Long categoryId) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isBlank() || price == null || price.signum() <= 0 || stock < 0) {
            throw new IllegalArgumentException("Tên bắt buộc; giá phải lớn hơn 0; tồn kho không được âm.");
        }
        Category category = getCategory(categoryId);
        boolean duplicate = id == null
                ? products.existsByNameIgnoreCaseAndCategoryId(normalizedName, categoryId)
                : products.existsByNameIgnoreCaseAndCategoryIdAndIdNot(normalizedName, categoryId, id);
        if (duplicate) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại trong danh mục.");
        }
        Product product = id == null ? new Product(normalizedName, price, stock, description, category)
                : getProduct(id);
        product.setName(normalizedName);
        product.setPrice(price);
        product.setStock(stock);
        product.setDescription(description == null ? "" : description.trim());
        product.setCategory(category);
        return products.save(product);
    }

    @Transactional
    public void deleteProduct(Long id, iuh.fit.demoprj.repository.OrderItemRepository orderItems) {
        getProduct(id);
        if (orderItems.existsByProductId(id)) {
            throw new IllegalArgumentException("Sản phẩm đã có trong đơn hàng, không thể xóa.");
        }
        products.deleteById(id);
    }
}

