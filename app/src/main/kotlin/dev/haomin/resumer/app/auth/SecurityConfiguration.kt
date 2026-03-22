package dev.haomin.resumer.app.auth

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = ["dev.haomin.resumer.app.auth.prop"])
class SecurityConfiguration {
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(
        userDetailsService: UserDetailsService, passwordEncoder: PasswordEncoder,
    ): AuthenticationManager =
        DaoAuthenticationProvider(userDetailsService)
            .apply { setPasswordEncoder(passwordEncoder) }
            .let { ProviderManager(it) }
}