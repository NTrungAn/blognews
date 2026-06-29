package com.blog.blogsystem.service;

import com.blog.blogsystem.dto.request.TagRequest;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.TagResponse;

import java.util.List;
import java.util.UUID;

public interface TagService {

    TagResponse createTag(TagRequest request);

    TagResponse updateTag(UUID id, TagRequest request);

    void deleteTag(UUID id);

    TagResponse getTagById(UUID id);

    TagResponse getTagBySlug(String slug);

    PageResponse<TagResponse> getAllTags(int pageNo, int pageSize, String sortBy, String sortDir);

    List<TagResponse> searchTags(String keyword);
}
