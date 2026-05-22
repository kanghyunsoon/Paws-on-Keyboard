package com.tour_diary.infra.config;

import java.util.concurrent.TimeUnit;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MongoClientConfig {

    @Bean
    MongoClientSettingsBuilderCustomizer fastLocalMongoFailure() {
        return settings -> settings
                .applyToClusterSettings(cluster -> cluster.serverSelectionTimeout(3, TimeUnit.SECONDS))
                .applyToSocketSettings(socket -> socket.connectTimeout(3, TimeUnit.SECONDS));
    }
}
