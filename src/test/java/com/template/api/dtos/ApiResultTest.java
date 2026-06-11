package com.template.api.dtos;

import com.template.api.dtos.core.ApiResult;
import com.template.api.http_errors.ApiErrorType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResultTest {

    @Test
    void okCarriesStatusCodeAndData() {
        ApiResult<String> response = ApiResult.ok("payload");

        assertThat(response.status()).isEqualTo(200);
        assertThat(response.code()).isEqualTo("OK");
        assertThat(response.message()).isEqualTo("OK");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.errors()).isNull();
        assertThat(response.summary()).isNull();
    }

    @Test
    void errorCarriesTypeStatusAndNullData() {
        ApiResult<Void> response = ApiResult.error(ApiErrorType.BAD_REQUEST, "Bad request");

        assertThat(response.status()).isEqualTo(400);
        assertThat(response.code()).isEqualTo("BAD_REQUEST");
        assertThat(response.message()).isEqualTo("Bad request");
        assertThat(response.data()).isNull();
    }
}
