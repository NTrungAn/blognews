package com.blog.blogsystem.mapper;

import com.blog.blogsystem.dto.request.PostRequest;
import com.blog.blogsystem.dto.response.PostResponse;
import com.blog.blogsystem.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PostMapper {

    // Ánh xạ từ Entity sang Response DTO
    // Lấy full_name của tác giả gán vào authorName
    // Lấy full_name của tác giả gán vào authorName
    @Mapping(source = "author.fullName", target = "authorName")
    @Mapping(source = "author.username", target = "authorUsername")
    PostResponse toResponse(Post post);

    // Ánh xạ từ Request DTO sang Entity (Bỏ qua các trường tự động tạo như ID, createdAt)
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "slug",        ignore = true) // slug được tự động tạo trong Service
    @Mapping(target = "author",      ignore = true)
    @Mapping(target = "category",    ignore = true)
    @Mapping(target = "tags",        ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "viewCount",   ignore = true)
    Post toEntity(PostRequest request);
}
