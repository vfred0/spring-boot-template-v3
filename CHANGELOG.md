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
