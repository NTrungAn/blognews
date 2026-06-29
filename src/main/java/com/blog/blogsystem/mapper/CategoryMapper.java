package com.blog.blogsystem.mapper;

import com.blog.blogsystem.dto.request.CategoryRequest;
import com.blog.blogsystem.dto.response.CategoryResponse;
import com.blog.blogsystem.entity.Category;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    // Ánh xạ từ Entity sang Response DTO
    CategoryResponse toResponse(Category category);

    // Ánh xạ từ Request DTO sang Entity (bỏ qua id, postCount sẽ được set thủ công)
    @Mapping(target = "id", ignore = true)
    Category toEntity(CategoryRequest request);

    /**
     * Cập nhật một entity đã có từ request DTO.
     * Chỉ cập nhật các trường không null (hữu ích cho PATCH partial update).
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(CategoryRequest request, @MappingTarget Category category);
}
