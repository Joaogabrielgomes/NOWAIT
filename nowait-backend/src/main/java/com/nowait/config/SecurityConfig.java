package com.nowait.config;

import com.nowait.filter.RateLimitGlobalFilter;
import com.nowait.filter.RateLimitUsuarioFilter;
import com.nowait.model.Perfil;
import com.nowait.security.JwtAuthenticationFilter;
import com.nowait.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigin;

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final RateLimitGlobalFilter   rateLimitGlobalFilter;
    private final RateLimitUsuarioFilter  rateLimitUsuarioFilter;
    private final UsuarioService          usuarioService;
    private final PasswordEncoder         passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login", "/api/auth/register",
                        "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cep/**").permitAll()
                .requestMatchers("/ws/**").permitAll()

                .requestMatchers(HttpMethod.GET, "/api/estabelecimentos").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/estabelecimentos/me").hasRole(Perfil.ESTABELECIMENTO)
                .requestMatchers(HttpMethod.GET, "/api/estabelecimentos/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/estabelecimentos/*/mesas").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/estabelecimentos").hasRole(Perfil.ESTABELECIMENTO)
                .requestMatchers(HttpMethod.PUT, "/api/estabelecimentos/**").hasRole(Perfil.ESTABELECIMENTO)

                .requestMatchers(HttpMethod.POST, "/api/estabelecimentos/*/mesas").hasRole(Perfil.ESTABELECIMENTO)
                .requestMatchers(HttpMethod.PUT, "/api/mesas/**").hasRole(Perfil.ESTABELECIMENTO)
                .requestMatchers(HttpMethod.PATCH, "/api/mesas/**").hasRole(Perfil.ESTABELECIMENTO)
                .requestMatchers(HttpMethod.DELETE, "/api/mesas/**").hasRole(Perfil.ESTABELECIMENTO)

                .requestMatchers(HttpMethod.POST, "/api/estabelecimentos/*/fila").hasRole(Perfil.CLIENTE)
                .requestMatchers(HttpMethod.GET, "/api/fila/me/ativa").hasRole(Perfil.CLIENTE)
                .requestMatchers(HttpMethod.GET, "/api/estabelecimentos/*/fila").hasRole(Perfil.ESTABELECIMENTO)
                .requestMatchers(HttpMethod.PATCH, "/api/fila/*/chamar").hasRole(Perfil.ESTABELECIMENTO)
                .requestMatchers(HttpMethod.PATCH, "/api/fila/*/atender").hasRole(Perfil.ESTABELECIMENTO)

                .requestMatchers(HttpMethod.POST, "/api/estabelecimentos/*/agendamentos").hasRole(Perfil.CLIENTE)
                .requestMatchers(HttpMethod.GET, "/api/agendamentos/me").hasRole(Perfil.CLIENTE)
                .requestMatchers(HttpMethod.GET, "/api/estabelecimentos/*/agendamentos").hasRole(Perfil.ESTABELECIMENTO)
                .requestMatchers(HttpMethod.PATCH, "/api/agendamentos/*/confirmar-chegada").hasRole(Perfil.ESTABELECIMENTO)

                .requestMatchers("/api/admin/**").hasRole(Perfil.ADMIN)

                .anyRequest().authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(rateLimitGlobalFilter,  UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitUsuarioFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter,           UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
