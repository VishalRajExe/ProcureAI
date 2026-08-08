package com.procureai.repository;

import com.procureai.entity.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long> {
    List<EmailMessage> findByNegotiationIdOrderByCreatedAtAsc(Long negotiationId);
}
