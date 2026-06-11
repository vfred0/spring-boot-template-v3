package com.template.service.core.shared;

import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.LocaleResolver;

import java.text.MessageFormat;
import java.util.Locale;

@Service
@AllArgsConstructor
public class MessageService {

    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public String getMessage(String code) {
        Locale locale = getCurrentLocale();
        return messageSource.getMessage(code, null, locale);
    }

    public String getMessage(String code, Object[] args) {
        Locale locale = getCurrentLocale();
        String pattern = messageSource.getMessage(code, null, locale);
        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }

    private Locale getCurrentLocale() {
        var requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            return localeResolver.resolveLocale(requestAttributes.getRequest());
        }
        return Locale.ENGLISH;
    }
}
