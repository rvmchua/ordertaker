package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.CustomerProfileRequest;
import com.rvmc.ordertaker.dto.CustomerProfileResponse;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.CustomerMapper;
import com.rvmc.ordertaker.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerProfileResponse findByCustomerId(Long customerId) {
        return customerRepository
                .findById(customerId)
                .map(customerMapper::toProfileResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    public CustomerProfileResponse editProfile(CustomerProfileRequest request) {
        return null;
    }
}
