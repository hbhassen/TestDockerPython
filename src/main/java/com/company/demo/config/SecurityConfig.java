package com.company.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Definit le modele de securite de base du service genere.
 *
 * <p>La configuration generee est volontairement simple : HTTP Basic stateless,
 * un utilisateur de demonstration et une liste blanche publique pour la sante
 * et la documentation API. Remplacez les identifiants de demonstration et faites
 * evoluer le mecanisme d authentification avant d exposer un vrai service hors
 * d un environnement controle.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/application/version"
    };

    /**
     * Applique les regles de securite HTTP utilisees par l API REST generee.
     *
     * @param http builder Spring Security mutable
     * @return la chaine de filtres configuree
     * @throws Exception si Spring Security ne parvient pas a construire la chaine
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    /**
     * Encodeur de mot de passe utilise pour l utilisateur en memoire de demarrage.
     *
     * @return encodeur BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Declare l utilisateur de demonstration qui protege tous les endpoints non publics.
     *
     * <p>Remplacez ce bean par votre integration de fournisseur d identite
     * reel lorsque le service depasse l etat de starter genere.
     *
     * @param passwordEncoder encodeur utilise pour hacher le mot de passe de demarrage
     * @return service de gestion des utilisateurs en memoire genere
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails adminUser = User.withUsername("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(adminUser);
    }
}
