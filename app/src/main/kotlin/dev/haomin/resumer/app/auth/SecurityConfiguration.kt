package dev.haomin.resumer.app.auth

import dev.haomin.resumer.app.auth.filter.JwtAuthFilter
import dev.haomin.resumer.app.auth.service.TokenService
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.HandlerExceptionResolver

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties
@ConfigurationPropertiesScan(basePackages = ["dev.haomin.resumer.app.auth.prop"])
class SecurityConfiguration(
    private val tokenService: TokenService,
    private val handlerExceptionResolver: HandlerExceptionResolver,
) {
    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(
        userDetailsService: UserDetailsService, passwordEncoder: PasswordEncoder,
    ): AuthenticationManager =
        DaoAuthenticationProvider(userDetailsService)
            .apply { setPasswordEncoder(passwordEncoder) }
            .let { ProviderManager(it) }

    /**
     * Security filter chain for authentication endpoints
     */
    @Bean
    @Order(1)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/api/auth/**", "/api/register/**")
            .authorizeHttpRequests { it
                .anyRequest().permitAll()
            }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .rememberMe { it.disable() }
            .sessionManagement { it
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .logout { it.disable() }
            .build()

    /**
     * General security filter chain for all other endpoints
     */
    @Bean
    @Order(2)
    fun generalFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/**")
            .authorizeHttpRequests { it
                .requestMatchers("/api/public/**").permitAll()
                .anyRequest().authenticated()
            }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .rememberMe { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .logout { it.disable() }
            .addFilterBefore(
                JwtAuthFilter(tokenService, handlerExceptionResolver),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .build()
}