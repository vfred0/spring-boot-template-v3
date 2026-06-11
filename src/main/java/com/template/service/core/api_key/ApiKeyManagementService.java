package com.template.service.core.api_key;

import lombok.RequiredArgsConstructor;
import com.template.api.http_errors.exceptions.ApiKeyNotFoundException;
import com.template.data.entities.core.ApiKey;
import com.template.data.daos.ApiKeyRepository;
import com.template.config.api_key.ApiKeyHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiKeyManagementService {

    private static final String PREFIX = "sk_live_";
    private static final int RAW_KEY_BYTES = 32;

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyHasher hasher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public IssuedApiKey issue(CreateApiKeyCommand command) {
        String rawKey = generateRawKey();

        ApiKey apiKey = new ApiKey();
        apiKey.setKeyHash(hasher.hash(rawKey));
        apiKey.setPrefixHint(buildHint(rawKey));
        apiKey.setSubject(command.subject());
        apiKey.setLabel(command.label());
        apiKey.setAllowedIps(command.allowedIps());
        apiKey.setExpiresAt(command.expiresAt());
        apiKey.setCreatedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        return new IssuedApiKey(apiKey.getId(), rawKey, apiKey.getPrefixHint());
    }

    @Transactional
    public void revoke(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new ApiKeyNotFoundException(id));
        apiKey.setRevokedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public List<ApiKey> listBySubject(String subject) {
        return apiKeyRepository.findBySubject(subject);
    }

    private String generateRawKey() {
        byte[] bytes = new byte[RAW_KEY_BYTES];
        secureRandom.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildHint(String rawKey) {
        return PREFIX + "..." + rawKey.substring(rawKey.length() - 4);
    }
}
