package com.template.config.mapper;

import com.template.api.dtos.account.AccountResponse;
import com.template.data.entities.core.rbac.Account;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AccountMapper {
    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    @Mapping(target = "accountId", source = "id")
    @Mapping(target = "clientId", source = "client.id")
    AccountResponse toResponse(Account account);
}
