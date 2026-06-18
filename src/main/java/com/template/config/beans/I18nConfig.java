package com.template.config.beans;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class I18nConfig {

    private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("es");

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(DEFAULT_LOCALE);
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(DEFAULT_LOCALE);
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        ResourceBundleMessageSource validationMessages = new ResourceBundleMessageSource();
        validationMessages.setBasename("messages");
        validationMessages.setDefaultEncoding("UTF-8");
        validationMessages.setDefaultLocale(DEFAULT_LOCALE);
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(validationMessages);
        return bean;
    }
}
