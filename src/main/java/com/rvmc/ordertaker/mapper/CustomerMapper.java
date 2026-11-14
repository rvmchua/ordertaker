package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.CustomerProfileResponse;
import com.rvmc.ordertaker.entity.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerProfileResponse toProfileResponse(Customer customer);
}
