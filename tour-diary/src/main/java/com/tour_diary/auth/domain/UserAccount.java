package com.tour_diary.auth.domain;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_accounts")
public record UserAccount(
        @Id String id,
        @Indexed(unique = true) String email,
        String name,
        String passwordHash,
        Instant createdAt
) {
}
