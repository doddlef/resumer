package dev.haomin.resumer.app

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = ["dev.haomin.resumer.app.auth.prop"])
class SecurityConfiguration {
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()
}
