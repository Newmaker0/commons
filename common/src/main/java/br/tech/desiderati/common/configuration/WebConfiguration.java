/*
 * Copyright (c) 2019 - Felipe Desiderati
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
package br.tech.desiderati.common.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.NonNull;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

@SpringBootConfiguration
@ConditionalOnWebApplication
@ConditionalOnSingleCandidate(WebConfiguration.class)
public class WebConfiguration implements WebMvcConfigurer {

    @Value("${app.api-base-path:/api}")
    private String apiBasePath;

    private LocalValidatorFactoryBean validatorFactory;

    @Autowired
    public WebConfiguration(@Qualifier("localValidatorFactoryBean") LocalValidatorFactoryBean validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    /**
     * @return O caminho raiz da aplicação.
     */
    @SuppressWarnings("WeakerAccess") // Must be public!
    public String getDefaultApiBasePath() {
        if (!apiBasePath.startsWith("/")) {
            apiBasePath = "/" + apiBasePath;
        }

        return apiBasePath;
    }

    /**
     * Specify a custom Spring MessageSource for resolving validation messages,
     * instead of relying on JSR-303's default "ValidationMessages.properties"
     * bundle in the classpath.
     */
    @Override
    public Validator getValidator() {
        return validatorFactory;
    }

    /**
     * Configuramos o Cross-Origin Resource Sharing (CORS).
     * <p>
     * Global CORS configuration
     * https://docs.spring.io/spring/docs/4.3.11.RELEASE/spring-framework-reference/html/cors.html#_global_cors_configuration
     */
    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping(getDefaultApiBasePath() + "/**")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedOrigins("*");
    }

    /**
     * Garante que todos os RESTs serão prefixados com /api.
     * Usar as versões /v1,/v2,... dentro dos próprios {@link RestController}.
     */
    @Bean
    public WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
        return new WebMvcRegistrations() {

            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return new RequestMappingHandlerMapping() {

                    @Override
                    protected void registerHandlerMethod(@NonNull Object handler, @NonNull Method method,
                                                         @NonNull RequestMappingInfo mapping) {

                        Class<?> beanType = method.getDeclaringClass();
                        RestController restApiController = beanType.getAnnotation(RestController.class);
                        if (restApiController != null) {
                            PatternsRequestCondition apiPattern = new PatternsRequestCondition(getDefaultApiBasePath())
                                .combine(mapping.getPatternsCondition());

                            mapping = new RequestMappingInfo(mapping.getName(), apiPattern,
                                mapping.getMethodsCondition(), mapping.getParamsCondition(),
                                mapping.getHeadersCondition(), mapping.getConsumesCondition(),
                                mapping.getProducesCondition(), mapping.getCustomCondition());
                        }
                        super.registerHandlerMethod(handler, method, mapping);
                    }
                };
            }
        };
    }
}
