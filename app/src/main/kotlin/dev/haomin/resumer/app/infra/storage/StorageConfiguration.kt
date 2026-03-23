package dev.haomin.resumer.app.infra.storage

import dev.haomin.resumer.app.infra.storage.local.LocalFileEngine
import dev.haomin.resumer.app.infra.storage.local.LocalFileUrlTokenCodec
import dev.haomin.resumer.app.infra.storage.prop.StorageProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "resumer.storage", name = ["engine"], havingValue = "local", matchIfMissing = true)
    fun localFileUrlTokenCodec(properties: StorageProperties): LocalFileUrlTokenCodec =
        LocalFileUrlTokenCodec(properties.local)

    @Bean
    @ConditionalOnProperty(prefix = "resumer.storage", name = ["engine"], havingValue = "local", matchIfMissing = true)
    fun localFileEngine(
        properties: StorageProperties,
        tokenCodec: LocalFileUrlTokenCodec,
    ): LocalFileEngine = LocalFileEngine(properties.local, tokenCodec)

    @Bean
    @ConditionalOnProperty(prefix = "resumer.storage", name = ["engine"], havingValue = "local", matchIfMissing = true)
    fun fileEngine(localFileEngine: LocalFileEngine): FileEngine = localFileEngine
}
