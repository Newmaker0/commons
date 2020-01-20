/*
 * Copyright (c) 2020 - Felipe Desiderati
 *  
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *  
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *  
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.herd.common.security.sign_request.authorization;

import com.squareup.okhttp.Request;
import io.herd.common.exception.ApplicationException;
import io.herd.common.security.sign_request.authorization.SignRequestAuthorizationClientProperties.SignValidation;
import io.herd.common.validation.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import static io.herd.common.security.sign_request.authorization.SignRequestSigner.HEADER_DATE;
import static io.herd.common.security.sign_request.authorization.SignRequestSigner.HEADER_DATE_FORMAT;

@Slf4j
@Service
@ConditionalOnProperty(name = "security.sign-request.authorization.enabled", havingValue = "true")
public class SignRequestAuthorizationService {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_SIGNED_REQUEST = "SIGNED_REQUEST";

    // TODO Felipe Desiderati: Externalize???
    private static final int VALID_TIME_WINDOW = 15; // Minutes

    private final SignRequestAuthorizationClientProperties signRequestAuthorizationClientProperties;

    private final SignRequestAuthorizedClientRepository signRequestAuthorizedClientRepository;

    @Autowired
    public SignRequestAuthorizationService(SignRequestAuthorizationClientProperties signRequestAuthorizationClientProperties,
                                           SignRequestAuthorizedClientRepository signRequestAuthorizedClientRepository) {
        this.signRequestAuthorizationClientProperties = signRequestAuthorizationClientProperties;
        this.signRequestAuthorizedClientRepository = signRequestAuthorizedClientRepository;
    }

    /**
     * Method responsible for signing the request using API Secret and API Id. Used commonly by the Swagger Clients.
     */
    @SuppressWarnings("unused")
    public Request sign(Request request) {
        try {
            ValidationUtils.validate(signRequestAuthorizationClientProperties, SignValidation.class);

            SimpleDateFormat dateFormat = new SimpleDateFormat(HEADER_DATE_FORMAT, Locale.US);
            Request newRequest = request.newBuilder()
                .addHeader(HEADER_DATE, dateFormat.format(new Date()))
                .build();

            String sign = SignRequestSigner.builder().request(newRequest)
                .secret(signRequestAuthorizationClientProperties.getSecretKey()).build().sign();

            newRequest = newRequest.newBuilder()
                .addHeader(HEADER_AUTHORIZATION, TOKEN_SIGNED_REQUEST + " " + signRequestAuthorizationClientProperties.getId() + ":" + sign)
                .build();
            return newRequest;
        } catch (Exception e) {
            throw new AuthenticationServiceException("Unable to sign the request due to: " + e.getMessage(), e);
        }
    }

    /**
     * Checks whether the current requisition is authorized to be executed. If so, the system (client)
     * accessing this API is added to the current request with the appropriate permissions.
     */
    public Authentication verifySignature(HttpServletRequest request) {
        try {
            final String auth = request.getHeader(HEADER_AUTHORIZATION);
            if (auth != null && auth.startsWith(TOKEN_SIGNED_REQUEST) && verifyDate(request)) {
                String[] authArr = auth.replace(TOKEN_SIGNED_REQUEST + " ", "").split(":");

                final String keyId = authArr[0];
                final String sign = authArr[1];
                final SignRequestAuthorizedClient authorizedClient =
                    signRequestAuthorizedClientRepository.findById(UUID.fromString(keyId))
                        .orElseThrow(() -> new ApplicationException("API Key not registered for Id: " + keyId));

                final String verifySign = SignRequestSigner.builder().httpServletRequest(request)
                    .secret(authorizedClient.getSecretKey()).build().sign();
                if (verifySign.equals(sign)) {
                    return new UsernamePasswordAuthenticationToken("API_USER", null,
                        AuthorityUtils.createAuthorityList(
                            authorizedClient.getRoles().stream().map(role -> "ROLE_" + role).toArray(String[]::new)));
                }
            }
            return null;
        } catch (Exception e) {
            throw new AuthenticationServiceException("Unable to authenticate the request due to: " + e.getMessage(), e);
        }
    }

    private boolean verifyDate(HttpServletRequest request) {
        if (request.getHeader(HEADER_DATE) == null) {
            return false;
        }

        ZonedDateTime date =
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(request.getDateHeader(HEADER_DATE)), ZoneId.of("UTC"));
        boolean dateOutOfRange = date.isBefore(ZonedDateTime.now().minus(VALID_TIME_WINDOW, ChronoUnit.MINUTES))
            || date.isAfter(ZonedDateTime.now().plus(VALID_TIME_WINDOW, ChronoUnit.MINUTES));
        return !dateOutOfRange;
    }
}
