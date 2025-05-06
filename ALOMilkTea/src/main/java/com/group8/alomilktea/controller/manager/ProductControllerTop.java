package com.group8.alomilktea.controller.manager;

import com.group8.alomilktea.repository.BestSellingProductDTO;
import com.group8.alomilktea.service.impl.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "Operations related to product management")
public class ProductControllerTop {
    @Autowired
    private ProductService productService;

    @GetMapping("/top-best-selling")
    @Operation(summary = "Get top best selling products", description = "Returns the products with the highest sales")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the top best selling products",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = BestSellingProductDTO.class))),
            @ApiResponse(responseCode = "404", description = "No products found", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<List<BestSellingProductDTO>> getTopBestSellingProducts() {
        List<BestSellingProductDTO> products = productService.getTop6BestSellingProducts();
        return ResponseEntity.ok(products);
    }
}
