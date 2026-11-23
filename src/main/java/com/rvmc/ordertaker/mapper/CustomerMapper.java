package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.CustomerProfileResponse;
import com.rvmc.ordertaker.entity.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    CustomerProfileResponse toProfileResponse(Customer customer);
}
