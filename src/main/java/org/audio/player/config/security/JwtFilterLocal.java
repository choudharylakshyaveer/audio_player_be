package org.audio.player.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Profile("!prod")
@Component
public class JwtFilterLocal extends OncePerRequestFilter {

    private final SecretKey secretKey;
    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/stream/flac/",
            "/saveTrackMetadata",
            "/actuator/",
            "/public/"
    );
    public JwtFilterLocal(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            filterChain.doFilter(request, response);
            return;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String header = httpRequest.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("Missing or invalid Authorization header");
            return;
        }

        String token = header.substring(7);

        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            String username = claims.getSubject();
            Object roles = claims.get("roles");

            request.setAttribute("username", username);
            request.setAttribute("roles", roles);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("Invalid token: " + e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
