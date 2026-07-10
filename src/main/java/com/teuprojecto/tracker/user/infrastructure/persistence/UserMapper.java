package com.teuprojecto.tracker.user.infrastructure.persistence;

import com.teuprojecto.tracker.user.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserJpaEntity toJpa(User user);

    User toDomain(UserJpaEntity entity);
}
