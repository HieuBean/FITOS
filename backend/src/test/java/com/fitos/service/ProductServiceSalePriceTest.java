package com.fitos.service;

import com.fitos.dto.retail.RetailDtos.ProductRequest;
import com.fitos.exception.BadRequestException;
import com.fitos.model.Product;
import com.fitos.repository.ProductCategoryRepository;
import com.fitos.repository.ProductRepository;
import com.fitos.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceSalePriceTest {
    @Mock ProductRepository products;
    @Mock ProductCategoryRepository categories;
    @InjectMocks ProductService service;

    @BeforeEach
    void authentication() {
        UserPrincipal principal = new UserPrincipal(1L, "manager@fitos.test", "", "Manager", true,
                List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_MANAGER")), 1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void cannotActivateUnpricedProduct() {
        Product product = Product.builder().id(7L).studioId(1L).sku("NEW-7").name("New")
                .costPrice(BigDecimal.ZERO).sellPrice(BigDecimal.ZERO).isActive(false).build();
        when(products.findByIdAndStudioId(7L, 1L)).thenReturn(Optional.of(product));
        assertThrows(BadRequestException.class, () -> service.toggleProduct(7L));
    }

    @Test
    void cannotCreateActiveProductWithoutSalePrice() {
        ProductRequest request = ProductRequest.builder().sku("NEW-8").name("New")
                .costPrice(BigDecimal.ZERO).sellPrice(BigDecimal.ZERO).isActive(true).build();
        assertThrows(BadRequestException.class, () -> service.createProduct(request));
    }
}
