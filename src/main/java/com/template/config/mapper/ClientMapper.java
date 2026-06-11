package com.template.config.mapper;

import com.template.api.dtos.client.ClientResponse;
import com.template.api.dtos.client.CreateClientRequest;
import com.template.data.entities.core.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    ClientMapper INSTANCE = Mappers.getMapper(ClientMapper.class);

    @Mapping(target = "id", ignore = true)
    Client toEntity(CreateClientRequest request);

    ClientResponse toResponse(Client client);
}