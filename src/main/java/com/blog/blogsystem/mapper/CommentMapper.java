package com.blog.blogsystem.mapper;

import com.blog.blogsystem.dto.request.CommentRequest;
import com.blog.blogsystem.dto.response.CommentResponse;
import com.blog.blogsystem.entity.Comment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    /**
     * Entity → Response DTO.
     * - author.id, author.username, author.fullName → author (nested DTO)
     * - parent.id → parentId
     * - replies → replies (đệ quy, MapStruct tự xử lý)
     */
    @Mapping(source = "author.id", target = "author.id")
    @Mapping(source = "author.username", target = "author.username")
    @Mapping(source = "author.fullName", target = "author.fullName")
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(target = "reactionsCount", ignore = true)
    CommentResponse toResponse(Comment comment);

    @org.mapstruct.AfterMapping
    default void mapReactionsCount(Comment comment, @org.mapstruct.MappingTarget CommentResponse response) {
        if (comment.getReactions() != null) {
            java.util.Map<String, Long> counts = comment.getReactions().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            com.blog.blogsystem.entity.CommentReaction::getEmoji,
                            java.util.stream.Collectors.counting()));
            response.setReactionsCount(counts);
        }
    }

    /**
     * Request DTO → Entity (bỏ qua các trường được thiết lập thủ công trong
     * Service).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "post", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Comment toEntity(CommentRequest request);
}
