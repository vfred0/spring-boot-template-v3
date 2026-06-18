# 0.3.0 (2026-06-17)

### Spring Boot Template

 * feature  **i18n:** add bilingual error responses with title field in ApiResult ([73fd074](https://github.com/vfred0/spring-boot-template-v3/commit/73fd074))
 * feature  **cors:** add CORS support across all security filter chains ([f0443c1](https://github.com/vfred0/spring-boot-template-v3/commit/f0443c1))
 * refactor  **auth:** move client credentials from request DTOs to server properties ([6096b8f](https://github.com/vfred0/spring-boot-template-v3/commit/6096b8f))
 * bug fix  **auth:** handle OAuth2 error callback and add public authorization URL ([9295bac](https://github.com/vfred0/spring-boot-template-v3/commit/9295bac))
 * bug fix  **auth:** use SameSite=Lax for refresh token cookie on OAuth redirect ([e7fe0f1](https://github.com/vfred0/spring-boot-template-v3/commit/e7fe0f1))
 * chore  **config:** update application properties, Docker Compose and Keycloak realm ([03a860e](https://github.com/vfred0/spring-boot-template-v3/commit/03a860e))

# 0.2.0 (2026-06-14)

### Spring Boot Template

 * feature  **auth:** add Google OAuth2 authorization code flow via Keycloak broker ([c7257f1](https://github.com/vfred0/spring-boot-template-v3/commit/c7257f1))
 * feature  **rbac:** expose JWT given_name claim in /api/me profile response ([d7ed229](https://github.com/vfred0/spring-boot-template-v3/commit/d7ed229))
 * refactor  **db:** consolidate V1-V7 Flyway migrations into V1__init.sql with schema isolation ([77ae43d](https://github.com/vfred0/spring-boot-template-v3/commit/77ae43d))
 * refactor  **db:** replace demo seeds with mode-independent admin bootstrap ([6b5d6d0](https://github.com/vfred0/spring-boot-template-v3/commit/6b5d6d0))
 * refactor  **entities:** align @Table annotations to bounded-context DB schemas ([49ac447](https://github.com/vfred0/spring-boot-template-v3/commit/49ac447))
 * bug fix  **config:** resolve Flyway locations and Quartz tablePrefix from properties ([4f00106](https://github.com/vfred0/spring-boot-template-v3/commit/4f00106))
 * chore  **config:** extract Swagger and logging config into dedicated YAML files ([78e5a3b](https://github.com/vfred0/spring-boot-template-v3/commit/78e5a3b))
 * chore  **infra:** mount host log volume and fix Docker app user permissions ([2702d4e](https://github.com/vfred0/spring-boot-template-v3/commit/2702d4e))
 * docs  **requests:** rewrite rbac.http as two-actor walkthrough and update dev env ([bee838d](https://github.com/vfred0/spring-boot-template-v3/commit/bee838d))

# 0.1.2 (2026-06-13)

### Spring Boot Template

 * bug fix  **requests:** repair invalid JSON payload in the api_keys RBAC walkthrough ([a50c22c](https://github.com/vfred0/spring-boot-template-v3/commit/a50c22c))
 * docs  **requests:** add RBAC management walkthrough for the keycloak-jwt profile ([2a7846f](https://github.com/vfred0/spring-boot-template-v3/commit/2a7846f))
 * docs  **requests:** move keycloak auth walkthrough to keycloak-jwt and align naming ([3158f19](https://github.com/vfred0/spring-boot-template-v3/commit/3158f19))
 * chore  **docker:** default compose security mode to KEYCLOAK_JWT ([6f96828](https://github.com/vfred0/spring-boot-template-v3/commit/6f96828))

# 0.1.1 (2026-06-11)

### Spring Boot Template

 * refactor  **keycloak:** rename realm to spring-boot-template for consistent project naming ([57d09e1](https://github.com/vfred0/spring-boot-template-v3/commit/57d09e1))
 * docs  **keycloak:** beginner guide for realms and the three security modes ([326f344](https://github.com/vfred0/spring-boot-template-v3/commit/326f344))
 * chore  **docker:** set compose project name to spring-boot-template ([7408ef4](https://github.com/vfred0/spring-boot-template-v3/commit/7408ef4))

# 0.1.0 (2026-06-11)

### Spring Boot Template

 * feature  **auth:** OAuth2 resource server template with three security modes — Keycloak opaque introspection, Keycloak local JWT validation and API key — with refresh-token cookie flow, registration through the Keycloak Admin API, local RBAC, rate limiting and DPoP ([342c838](https://github.com/vfred0/spring-boot-template-v3/commit/342c838))
 * chore  **docker:** hardened container image and compose stack with Keycloak and PostgreSQL ([892f8b1](https://github.com/vfred0/spring-boot-template-v3/commit/892f8b1))
 * docs  **architecture:** PlantUML sequence diagrams for login, refresh, logout and authenticated request flows ([8ffe4a2](https://github.com/vfred0/spring-boot-template-v3/commit/8ffe4a2))
