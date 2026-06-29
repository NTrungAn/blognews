package com.blog.blogsystem.controller;

import com.blog.blogsystem.dto.request.TagRequest;
import com.blog.blogsystem.entity.Tag;
import com.blog.blogsystem.repository.TagRepository;
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
import static org.hamcrest.Matchers.hasItem;
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
public class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TagRepository tagRepository;

    // API public lấy danh sách tag có phân trang.
    @Test
    public void testGetAllTags_Success() throws Exception {
        Tag tag = saveTag("Frontend " + uniqueSuffix(), "frontend-" + uniqueSuffix());

        mockMvc.perform(get("/api/tags")
                .param("pageNo", "0")
                .param("pageSize", "10")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[*].slug").value(hasItem(tag.getSlug())));
    }

    // API public lấy chi tiết tag theo slug.
    @Test
    public void testGetTagBySlug_Success() throws Exception {
        Tag tag = saveTag("Backend " + uniqueSuffix(), "backend-" + uniqueSuffix());

        mockMvc.perform(get("/api/tags/slug/{slug}", tag.getSlug())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(tag.getId().toString()))
                .andExpect(jsonPath("$.data.slug").value(tag.getSlug()));
    }

    // API public tìm kiếm tag theo từ khóa trong tên.
    @Test
    public void testSearchTags_Success() throws Exception {
        String suffix = uniqueSuffix();
        Tag tag = saveTag("Search " + suffix, "search-" + suffix);

        mockMvc.perform(get("/api/tags/search")
                .param("q", suffix)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].slug").value(hasItem(tag.getSlug())));
    }

    // ADMIN tạo tag với dữ liệu sai định dạng phải bị lỗi validation.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testCreateTag_WithMockAdmin_ValidationFail() throws Exception {
        TagRequest request = new TagRequest();
        request.setName("");
        request.setSlug("Invalid Slug");

        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.name").value("Tên tag không được để trống"))
                .andExpect(jsonPath("$.data.slug", containsString("Slug chỉ được chứa chữ thường")));
    }

    // EDITOR được phép tạo tag thành công.
    @Test
    @WithMockUser(username = "editor", roles = {"EDITOR"})
    public void testCreateTag_WithMockEditor_Success() throws Exception {
        String suffix = uniqueSuffix();
        TagRequest request = new TagRequest();
        request.setName("Tag " + suffix);
        request.setSlug("tag-" + suffix);

        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.name").value("Tag " + suffix))
                .andExpect(jsonPath("$.data.slug").value("tag-" + suffix));
    }

    // READER không có quyền tạo tag nên phải bị chặn 403.
    @Test
    @WithMockUser(username = "reader", roles = {"READER"})
    public void testCreateTag_WithMockReader_Forbidden() throws Exception {
        TagRequest request = new TagRequest();
        request.setName("Forbidden Tag");
        request.setSlug("forbidden-tag");

        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này"));
    }

    // ADMIN cập nhật tag thành công với name/slug mới.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testUpdateTag_WithMockAdmin_Success() throws Exception {
        Tag tag = saveTag("Original Tag " + uniqueSuffix(), "original-tag-" + uniqueSuffix());

        TagRequest request = new TagRequest();
        request.setName("Updated Tag");
        request.setSlug("updated-tag");

        mockMvc.perform(put("/api/tags/{id}", tag.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Updated Tag"))
                .andExpect(jsonPath("$.data.slug").value("updated-tag"));
    }

    // ADMIN xóa tag thành công khi tag tồn tại.
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void testDeleteTag_WithMockAdmin_Success() throws Exception {
        Tag tag = saveTag("Delete Tag " + uniqueSuffix(), "delete-tag-" + uniqueSuffix());

        mockMvc.perform(delete("/api/tags/{id}", tag.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data").value("Xóa tag thành công!"));
    }

    private Tag saveTag(String name, String slug) {
        return tagRepository.save(Tag.builder()
                .name(name)
                .slug(slug)
                .build());
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}
