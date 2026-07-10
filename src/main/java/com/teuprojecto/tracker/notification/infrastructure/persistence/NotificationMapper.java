package com.teuprojecto.tracker.notification.infrastructure.persistence;

import com.teuprojecto.tracker.notification.domain.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "recipientId", source = "recipient.id")
    NotificationJpaEntity toJpa(Notification notification);

    @Mapping(target = "recipient", ignore = true)
    Notification toDomain(NotificationJpaEntity entity);
}
