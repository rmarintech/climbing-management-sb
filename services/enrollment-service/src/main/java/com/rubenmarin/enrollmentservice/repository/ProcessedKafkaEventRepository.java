package com.rubenmarin.enrollmentservice.repository;

import com.rubenmarin.enrollmentservice.document.ProcessedKafkaEventDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface ProcessedKafkaEventRepository
        extends MongoRepository<ProcessedKafkaEventDocument, UUID> {
}