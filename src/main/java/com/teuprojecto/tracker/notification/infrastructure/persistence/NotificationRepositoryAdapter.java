package com.teuprojecto.tracker.notification.infrastructure.persistence;

import com.teuprojecto.tracker.notification.domain.Notification;
import com.teuprojecto.tracker.notification.domain.NotificationRepository;
import com.teuprojecto.tracker.user.infrastructure.persistence.UserJpaRepository;
import com.teuprojecto.tracker.user.infrastructure.persistence.UserMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;
    private final NotificationMapper notificationMapper;
    private final UserJpaRepository userJpaRepository;
    private final UserMapper userMapper;

    public NotificationRepositoryAdapter(NotificationJpaRepository jpaRepository,
                                         NotificationMapper notificationMapper,
                                         UserJpaRepository userJpaRepository, UserMapper userMapper) {
        this.jpaRepository = jpaRepository;
        this.notificationMapper = notificationMapper;
        this.userJpaRepository = userJpaRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Notification save(Notification notification) {
        var entity = notificationMapper.toJpa(notification);
        var saved = jpaRepository.save(entity);
        return toDomainWithAssociations(saved);
    }

    @Override
    public Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable) {
        return jpaRepository.findByRecipientId(recipientId, pageable)
                .map(this::toDomainWithAssociations);
    }

    private Notification toDomainWithAssociations(NotificationJpaEntity entity) {
        var partial = notificationMapper.toDomain(entity);
        var recipient = userJpaRepository.findById(entity.getRecipientId())
                .map(userMapper::toDomain)
                .orElse(null);
        return new Notification(
                partial.getId(),
                recipient,
                partial.getMessage(),
                partial.getType(),
                partial.getStatus(),
                partial.getCreatedAt()
        );
    }
}
