package com.group8.alomilktea.controller.admin;

import com.group8.alomilktea.common.enums.ProductAttribute;
import com.group8.alomilktea.entity.Category;
import com.group8.alomilktea.entity.Product;
import com.group8.alomilktea.entity.ProductDetail;
import com.group8.alomilktea.entity.Promotion;
import com.group8.alomilktea.service.ICategoryService;
import com.group8.alomilktea.service.IProductDetailService;
import com.group8.alomilktea.service.IProductService;
import com.group8.alomilktea.service.IPromotionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/product")
public class ProductController {

    @Autowired
    private IProductService productService;

    @Autowired
    private ICategoryService categoryService;

    @Autowired
    private IPromotionService promotionService;

    @Autowired
    private IProductDetailService productDetailService;

    @GetMapping("/list")
    public ResponseEntity<?> showProductList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {

        Page<Product> productPage = productService.getAll(PageRequest.of(page, size));
        List<Product> products = productPage.getContent();

        Map<Integer, String> productCategoryMap = products.stream()
                .collect(Collectors.toMap(
                        Product::getProId,
                        product -> {
                            String categoryName = productService.getCategoryNameByProductId(product.getProId());
                            return categoryName != null ? categoryName : "No Category";
                        }));

        Map<Integer, String> productPromotionMap = products.stream()
                .collect(Collectors.toMap(
                        Product::getProId,
                        product -> {
                            String promotionName = productService.getPromotionNameByProductId(product.getProId());
                            return promotionName != null ? promotionName : "No Promotion";
                        }));

        Map<String, Object> response = new HashMap<>();
        response.put("products", products);
        response.put("productCategoryMap", productCategoryMap);
        response.put("productPromotionMap", productPromotionMap);
        response.put("currentPage", page);
        response.put("totalPages", productPage.getTotalPages());
        response.put("totalItems", productPage.getTotalElements());
        response.put("pageSize", size);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<?> getProductDetails(@PathVariable Integer id) {
        Product product = productService.findById(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    @GetMapping("/details/edit/{prodid}")
    public ResponseEntity<?> editProductDetail(@PathVariable Integer prodid) {
        ProductDetail productDetail = productDetailService.findById(prodid);
        if (productDetail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(productDetail);
    }

    @PostMapping("/details/edit/save")
    public ResponseEntity<?> saveProductDetail(
            @RequestParam("prodId") Integer prodId,
            @RequestParam("price") Double price) {
        
        ProductDetail productDetail = productDetailService.findById(prodId);
        if (productDetail == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product not found"));
        }

        if (price == null || price <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid price"));
        }

        productDetail.setPrice(price);
        productDetailService.save(productDetail);

        return ResponseEntity.ok(Map.of(
            "message", "Product detail updated successfully",
            "productDetail", productDetail
        ));
    }

    @GetMapping("/add")
    public ResponseEntity<?> addProduct() {
        List<Category> categories = categoryService.findAll();
        List<Promotion> promotions = promotionService.findAll();

        Map<String, Object> response = new HashMap<>();
        response.put("categories", categories);
        response.put("promotions", promotions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/add/save")
    public ResponseEntity<?> saveProduct(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "promotionId", required = false) Integer promotionId,
            @RequestParam("categoryId") Integer categoryId) {

        Product product = new Product();
        product.setName(name);
        product.setDescription(description);

        Promotion promotion = (promotionId != null) ? promotionService.findById(promotionId) : null;
        Category category = (categoryId != null) ? categoryService.findById(categoryId) : null;

        product.setPromotion(promotion);
        product.setCategory(category);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
                product.setImageLink("/uploads/" + fileName);
                imageFile.transferTo(new File("uploads/" + fileName));
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload image"));
            }
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            product.setImageLink(imageUrl);
        }

        List<ProductDetail> productDetails = List.of(
                new ProductDetail(null, ProductAttribute.S, null, product),
                new ProductDetail(null, ProductAttribute.M, null, product),
                new ProductDetail(null, ProductAttribute.L, null, product)
        );

        product.setProductDetails(productDetails);
        productService.save(product);

        return ResponseEntity.ok(Map.of(
            "message", "Product created successfully",
            "product", product
        ));
    }

    @GetMapping("/edit/{proId}")
    public ResponseEntity<?> editProduct(@PathVariable Integer proId) {
        Product product = productService.findById(proId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        List<Category> categories = categoryService.findAll();
        List<Promotion> promotions = promotionService.findAll();

        Map<String, Object> response = new HashMap<>();
        response.put("product", product);
        response.put("categories", categories);
        response.put("promotions", promotions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/edit/save")
    public ResponseEntity<?> updateProduct(
            @RequestParam("id") Integer id,
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam("promotionId") Integer promotionId,
            @RequestParam("categoryId") Integer categoryId) {

        Product product = productService.findById(id);
        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product not found"));
        }

        product.setName(name);
        product.setDescription(description);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String fileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
                product.setImageLink("/uploads/" + fileName);
                imageFile.transferTo(new File("uploads/" + fileName));
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Failed to upload image"));
            }
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            product.setImageLink(imageUrl);
        }

        Category category = categoryService.findById(categoryId);
        Promotion promotion = promotionService.findById(promotionId);

        if (category == null || promotion == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid category or promotion ID"));
        }

        product.setCategory(category);
        product.setPromotion(promotion);
        productService.save(product);

        return ResponseEntity.ok(Map.of(
            "message", "Product updated successfully",
            "product", product
        ));
    }

    @PostMapping("/delete/{proId}")
    public ResponseEntity<?> deleteProduct(@PathVariable("proId") Integer proId) {
        Product product = productService.findById(proId);

        if (product == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Product not found"));
        }

        productService.deleteById(proId);

        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }
}
