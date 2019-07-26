/*
 * Copyright (c) 2019 - Felipe Desiderati ALL RIGHTS RESERVED.
 *
 * This software is protected by international copyright laws and cannot be
 * used, copied, stored or distributed without prior authorization.
 */
package br.tech.desiderati.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Slf4j
@Service
@SuppressWarnings("unused")
public class JwtService {

    private final JwtProperties jwtProperties;

    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final PrivateKey privateKey = loadPrivateKey();

    @Getter(lazy = true, value = AccessLevel.PRIVATE)
    private final PublicKey publicKey = loadPublicKey();

    @Autowired
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    private PrivateKey loadPrivateKey() {
        assert jwtProperties != null;
        try {
            if (StringUtils.isBlank(jwtProperties.getPrivateKey())) {
                throw new IllegalStateException("Empty private key");
            }
            byte[] encoded = Base64.getDecoder().decode(jwtProperties.getPrivateKey());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JwtException("Unable to load private key", e);
        }
    }

    private PublicKey loadPublicKey() {
        assert jwtProperties != null;
        try {
            if (StringUtils.isBlank(jwtProperties.getPublicKey())) {
                throw new IllegalStateException("Empty public key");
            }
            byte[] econdedPublicKey = Base64.getDecoder().decode(jwtProperties.getPublicKey());
            X509EncodedKeySpec pkeySpec = new X509EncodedKeySpec(econdedPublicKey);
            return KeyFactory.getInstance("RSA").generatePublic(pkeySpec);
        } catch (Exception e) {
            throw new JwtException("Unable to load public key", e);
        }
    }

    public String generateToken(JwtTokenPayloadConfigurer configurer) {
        Claims tokenPayload = Jwts.claims();
        configurer.configure(tokenPayload);
        return Jwts.builder()
            .setClaims(tokenPayload)
            .signWith(SignatureAlgorithm.RS256, getPrivateKey())
            .compact();
    }

    public <T> T extractTokenPayload(String token, JwtTokenPayloadExtractor<T> extractor) {
        PublicKey innerPublicKey = getPublicKey();
        Claims tokenPayload = Jwts.parser()
            .setSigningKey(innerPublicKey)
            .parseClaimsJws(token)
            .getBody();
        return extractor.extract(tokenPayload);
    }
}
