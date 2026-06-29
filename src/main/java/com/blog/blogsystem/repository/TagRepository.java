package com.blog.blogsystem.repository;

import com.blog.blogsystem.entity.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByName(String name);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    List<Tag> findByNameContainingIgnoreCase(String keyword);

    Page<Tag> findAll(Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
        value = "SELECT t.id as id, t.name as name, t.slug as slug, COUNT(pt.post_id) as postCount " +
                "FROM tags t LEFT JOIN post_tags pt ON t.id = pt.tag_id " +
                "GROUP BY t.id, t.name, t.slug",
        countQuery = "SELECT COUNT(t.id) FROM tags t",
        nativeQuery = true
    )
    Page<com.blog.blogsystem.dto.projection.TagCountProjection> findAllWithPostCount(Pageable pageable);
}
