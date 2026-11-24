package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.CustomerProfileRequest;
import com.rvmc.ordertaker.dto.CustomerProfileResponse;
import com.rvmc.ordertaker.entity.Customer;
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

    /**
     * Finds a customer by identifier and maps the customer profile.
     *
     * @param customerId customer identifier
     * @return mapped customer profile
     */
    public CustomerProfileResponse findByCustomerId(Long customerId) {
        return customerRepository
                .findById(customerId)
                .map(customerMapper::toProfileResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
    }

    /**
     * Applies an updated customer profile request.
     *
     * @param request customer profile update
     * @return the updated customer profile
     * @deprecated profile editing is not implemented yet
     */
    public CustomerProfileResponse editProfile(CustomerProfileRequest request) {
        return null;
    }

    /**
     * Applies an update to the identified customer's mutable profile fields.
     *
     * @param customerId customer identifier
     * @param request profile fields to update
     * @return the updated customer profile
     * @throws ResourceNotFoundException if the customer does not exist
     */
    public CustomerProfileResponse editProfile(Long customerId, CustomerProfileRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        if (request.customerName() != null) {
            customer.setCustomerName(request.customerName());
        }
        if (request.address1() != null) {
            customer.setAddress1(request.address1());
        }
        if (request.address2() != null) {
            customer.setAddress2(request.address2());
        }
        if (customer.getUser() != null) {
            if (request.username() != null) {
                customer.getUser().setUsername(request.username());
            }
            if (request.email() != null) {
                customer.getUser().setEmail(request.email());
            }
        }
        return customerMapper.toProfileResponse(customerRepository.save(customer));
    }
}
