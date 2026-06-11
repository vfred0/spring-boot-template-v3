package com.template.service;

import com.template.service.core.shared.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.text.MessageFormat;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageServiceTest {

    @Test
    void messageFormat_works_correctly() {
        String pattern = "Client with id={0} not found";
        String result = MessageFormat.format(pattern, "123");
        System.out.println("Direct MessageFormat: " + result);
        assertEquals("Client with id=123 not found", result);
    }

    @Test
    void getMessage_withArray() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.ENGLISH);

        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

        MessageService messageService = new MessageService(messageSource, localeResolver);

        String message = messageService.getMessage("error.client.notFound", new Object[]{"123"});
        System.out.println("MessageService result: " + message);

        assertEquals("Client with id=123 not found", message);
    }

    @Test
    void getMessage_withoutArgs() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setDefaultLocale(Locale.ENGLISH);

        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();

        MessageService messageService = new MessageService(messageSource, localeResolver);

        String message = messageService.getMessage("error.validation.failed");
        System.out.println("MessageService result: " + message);

        assertEquals("Validation failed", message);
    }
}
