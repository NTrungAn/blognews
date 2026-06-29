package com.blog.blogsystem.mapper;

import com.blog.blogsystem.dto.response.UserResponse;
import com.blog.blogsystem.entity.Role;
import com.blog.blogsystem.entity.User;
import com.blog.blogsystem.entity.enums.RoleType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "roles", qualifiedByName = "mapPrimaryRole")
    @Mapping(target = "biography", source = "biography")
    UserResponse toResponse(User user);

    @Named("mapPrimaryRole")
    default String mapPrimaryRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return RoleType.USER.name();
        }
        // Ưu tiên trả về ADMIN nếu có, nếu không trả về role đầu tiên
        boolean isAdmin = roles.stream().anyMatch(r -> r.getRoleName() == RoleType.ADMIN);
        return isAdmin ? RoleType.ADMIN.name() : roles.iterator().next().getRoleName().name();
    }
}
