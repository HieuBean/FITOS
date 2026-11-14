package com.fitos.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * Các bean bảo mật dùng chung.
 *
 * <p>Lưu ý: {@code PasswordEncoder} đã được khai báo duy nhất trong
 * {@link com.fitos.config.SecurityConfig}. Trước đây lớp này khai báo thêm một
 * bean cùng tên khiến Spring Boot dừng khởi động với
 * {@code BeanDefinitionOverrideException} (định nghĩa bean trùng, không cho ghi đè).
 * Không khai báo lại bean đó ở đây nữa.
 */
@Configuration
public class SecurityBeanConfig {
}
