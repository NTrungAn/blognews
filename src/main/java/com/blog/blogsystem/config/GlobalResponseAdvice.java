package com.blog.blogsystem.config;

import com.blog.blogsystem.dto.response.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.blog.blogsystem.controller")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Áp dụng cho tất cả các request
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // Nếu response đã là ApiResponse (ví dụ do GlobalExceptionHandler ném ra), giữ nguyên
        if (body instanceof ApiResponse) {
            return body;
        }

        // Lấy HTTP Status thực tế, mặc định 200 OK
        int status = HttpStatus.OK.value();
        if (response instanceof ServletServerHttpResponse) {
            status = ((ServletServerHttpResponse) response).getServletResponse().getStatus();
        }

        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .code(status)
                .message("Success")
                .data(body)
                .build();

        // Xử lý lỗi ClassCastException khi controller trả về String
        // Spring sẽ chọn StringHttpMessageConverter, converter này bắt buộc return type là String
        if (body instanceof String) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                // Phải set include NON_NULL để giống với @JsonInclude trên class ApiResponse
                mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
                return mapper.writeValueAsString(apiResponse);
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                return body;
            }
        }

        // Đóng gói dữ liệu vào ApiResponse
        return apiResponse;
    }
}
