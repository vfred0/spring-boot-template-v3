package com.template.service.core.operations.route;

import com.template.api.dtos.core.ApiResult;
import com.template.service.core.operations.Result;

import java.util.concurrent.CompletableFuture;

public sealed interface RouteResult permits RouteResult.Bulk, RouteResult.Single {

    record Bulk(CompletableFuture<? extends Result<?, ApiResult<Void>>> future)
            implements RouteResult {}

    record Single(Result<?, ApiResult<Void>> result)
            implements RouteResult {}
}
