package com.orcamento.notification.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.orcamento.notification.dto.NotificationEventDTO;
import com.orcamento.notification.entity.Notification;
import com.orcamento.notification.entity.enums.NotificationStatus;
import com.orcamento.notification.repository.NotificationRepository;
import com.orcamento.notification.service.EmailNotificationService;

@Component
public class NotificationConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationConsumer.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @RabbitListener(queues = "notifications")
    public void receive(NotificationEventDTO event) {
        LOGGER.info("Evento recebido do RabbitMQ para o orçamento {}", event.getExternalReferenceId());

        Notification notification = new Notification();
        notification.setExternalReferenceId(event.getExternalReferenceId());
        notification.setRecipientEmail(event.getRecipientEmail());
        notification.setRecipientName(event.getRecipientName());
        notification.setSubject(event.getSubject());
        notification.setBodyHtml(event.getBodyHtml());
        notification.setStatus(NotificationStatus.PENDING);

        notification = notificationRepository.save(notification);

        emailNotificationService.send(notification);
    }
}