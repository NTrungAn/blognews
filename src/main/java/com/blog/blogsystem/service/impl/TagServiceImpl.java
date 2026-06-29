package com.blog.blogsystem.service.impl;

import com.blog.blogsystem.dto.request.TagRequest;
import com.blog.blogsystem.dto.response.PageResponse;
import com.blog.blogsystem.dto.response.TagResponse;
import com.blog.blogsystem.entity.Tag;
import com.blog.blogsystem.mapper.TagMapper;
import com.blog.blogsystem.repository.TagRepository;
import com.blog.blogsystem.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    @Transactional
    public TagResponse createTag(TagRequest request) {
        // Kiểm tra trùng slug
        if (tagRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug '" + request.getSlug() + "' đã tồn tại.");
        }
        // Kiểm tra trùng tên
        if (tagRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Tag '" + request.getName() + "' đã tồn tại.");
        }

        Tag tag = tagMapper.toEntity(request);
        Tag saved = tagRepository.save(tag);
        return tagMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TagResponse updateTag(UUID id, TagRequest request) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Tag với id: " + id));

        // Kiểm tra slug trùng (ngoại trừ chính tag này)
        if (!tag.getSlug().equals(request.getSlug()) && tagRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug '" + request.getSlug() + "' đã được sử dụng.");
        }

        tag.setName(request.getName());
        tag.setSlug(request.getSlug());

        return tagMapper.toResponse(tagRepository.save(tag));
    }

    @Override
    @Transactional
    public void deleteTag(UUID id) {
        if (!tagRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy Tag với id: " + id);
        }
        tagRepository.deleteById(id);
    }

    @Override
    public TagResponse getTagById(UUID id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Tag với id: " + id));
        return tagMapper.toResponse(tag);
    }

    @Override
    public TagResponse getTagBySlug(String slug) {
        Tag tag = tagRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Tag với slug: " + slug));
        return tagMapper.toResponse(tag);
    }

    @Override
    public PageResponse<TagResponse> getAllTags(int pageNo, int pageSize, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);
        Page<com.blog.blogsystem.dto.projection.TagCountProjection> page = tagRepository.findAllWithPostCount(pageable);

        List<TagResponse> content = page.getContent().stream()
                .map(proj -> {
                    TagResponse res = new TagResponse();
                    res.setId(proj.getId());
                    res.setName(proj.getName());
                    res.setSlug(proj.getSlug());
                    res.setPostCount(proj.getPostCount());
                    return res;
                })
                .collect(Collectors.toList());

        return PageResponse.<TagResponse>builder()
                .content(content)
                .pageNo(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Override
    public List<TagResponse> searchTags(String keyword) {
        return tagRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }
}
