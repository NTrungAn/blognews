package com.blog.blogsystem.mapper;

import com.blog.blogsystem.dto.request.TagRequest;
import com.blog.blogsystem.dto.response.TagResponse;
import com.blog.blogsystem.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(Tag tag);

    @Mapping(target = "id", ignore = true)
    Tag toEntity(TagRequest request);
}
