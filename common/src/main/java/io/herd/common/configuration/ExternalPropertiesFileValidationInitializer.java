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
package io.herd.common.configuration;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.File;
import java.nio.file.Paths;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE) // Primeiro a ser executado!
public class ExternalPropertiesFileValidationInitializer implements ApplicationContextInitializer {

    @Override
    public void initialize(@NotNull ConfigurableApplicationContext applicationContext) {
        String externalProperties = applicationContext.getEnvironment().getProperty("spring.config.location");
        if (externalProperties != null) {
            externalProperties = externalProperties.replace("file:", "");
            File externalPropertiesFile = Paths.get(externalProperties).toFile();
            if (externalPropertiesFile.exists() && externalPropertiesFile.isFile()) {
                log.info("\n \nLoading external properties file:" + externalPropertiesFile + "\n");
            }
        }
    }
}