package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.CategoryRequest;
import com.blog.blogsystem.entity.Category;
import com.blog.blogsystem.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CategoryRepository categoryRepository;

    // API public lấy toàn bộ category và trả về danh sách đã được bọc trong ApiResponse.
    @Test
    public void testGetAllCategories_Success() throws Exception {
        Category category = saveCategory("Technology " + uniqueSuffix(), "technology-" + uniqueSuffix());

        mockMvc.perform(get("/api/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].slug").value(org.hamcrest.Matchers.hasItem(category.getSlug())));
    }

    // API public lấy chi tiết category theo slug hợp lệ.
    @Test
    public void testGetCategoryBySlug_Success() throws Exception {
        Category category = saveCategory("Business " + uniqueSuffix(), "business-" + uniqueSuffix());

        mockMvc.perform(get("/api/categories/slug/{slug}", category.getSlug())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(category.getId().toString()))
                .andExpect(jsonPath("$.data.slug").value(category.getSlug()));
    }

    // API public lấy category có phân trang và trả về đúng metadata paging.
    @Test
    public void testGetAllCategoriesPaged_Success() throws Exception {
        saveCategory("Lifestyle " + uniqueSuffix(), "lifestyle-" + uniqueSuffix());

        mockMvc.perform(get("/api/categories/paged")
                .param("pageNo", "0")
                .param("pageSize", "5")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.pageNo").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(5));
    }

    // ADMIN tạo category nhưng thiếu name nên phải bị lỗi validation 400.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateCategory_WithMockAdmin_ValidationFail() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setDescription("Mô tả danh mục test");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").value("Tên chuyên mục không được để trống"));
    }

    // ADMIN tạo category thành công, slug được sinh tự động từ name nếu không truyền vào.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateCategory_WithMockAdmin_Success() throws Exception {
        String suffix = uniqueSuffix();
        CategoryRequest request = new CategoryRequest();
        request.setName("Category Demo " + suffix);
        request.setDescription("Mô tả danh mục");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.name").value("Category Demo " + suffix))
                .andExpect(jsonPath("$.data.slug").value("category-demo-" + suffix.toLowerCase()));
    }

    // READER không có quyền tạo category nên phải bị chặn 403.
    @Test
    @WithMockUser(username = "reader", roles = {"READER"})
    public void testCreateCategory_WithMockReader_Forbidden() throws Exception {
        CategoryRequest request = new CategoryRequest();
        request.setName("Forbidden Category " + uniqueSuffix());
        request.setDescription("Không được phép tạo");

        mockMvc.perform(post("/api/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    // EDITOR được phép cập nhật category với dữ liệu hợp lệ.
    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    public void testUpdateCategory_WithMockEditor_Success() throws Exception {
        Category category = saveCategory("Original " + uniqueSuffix(), "original-" + uniqueSuffix());

        CategoryRequest request = new CategoryRequest();
        request.setName("Updated Category");
        request.setSlug("updated-category");
        request.setDescription("Đã cập nhật");

        mockMvc.perform(put("/api/categories/{id}", category.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated Category"))
                .andExpect(jsonPath("$.data.slug").value("updated-category"))
                .andExpect(jsonPath("$.data.description").value("Đã cập nhật"));
    }

    // ADMIN xóa category thành công khi category tồn tại và không vi phạm ràng buộc nghiệp vụ.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDeleteCategory_WithMockAdmin_Success() throws Exception {
        Category category = saveCategory("Delete " + uniqueSuffix(), "delete-" + uniqueSuffix());

        mockMvc.perform(delete("/api/categories/{id}", category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value("Xóa chuyên mục thành công!"));
    }

    // Cập nhật category với slug sai định dạng phải bị lỗi validation.
    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    public void testUpdateCategory_WithInvalidSlug_ValidationFail() throws Exception {
        Category category = saveCategory("Validation " + uniqueSuffix(), "validation-" + uniqueSuffix());

        CategoryRequest request = new CategoryRequest();
        request.setName("Validation Category");
        request.setSlug("Invalid Slug");

        mockMvc.perform(put("/api/categories/{id}", category.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.slug", containsString("Slug chỉ được chứa chữ thường")));
    }

    private Category saveCategory(String name, String slug) {
        Category category = Category.builder()
                .name(name)
                .slug(slug)
                .description("Category for integration test")
                .build();
        return categoryRepository.save(category);
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
