package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.CustomerProfileRequest;
import com.rvmc.ordertaker.dto.CustomerProfileResponse;
import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.exception.DuplicateEmailException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.CustomerMapper;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final UserRepository userRepository;

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
     * Applies an update to the identified customer's mutable profile fields.
     *
     * @param customerId customer identifier
     * @param request profile fields to update
     * @return the updated customer profile
     * @throws ResourceNotFoundException if the customer does not exist
     * @throws DuplicateEmailException if the email belongs to another user
     */
    @Transactional
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
        if (request.username() != null || request.email() != null) {
            if (customer.getUser() == null) {
                throw new IllegalStateException("Customer account is missing its user identity.");
            }
            if (request.username() != null) {
                customer.getUser().setUsername(request.username());
            }
            if (request.email() != null) {
                String email = request.email().trim().toLowerCase(Locale.ROOT);
                if (userRepository.existsByEmailAndUserIdNot(email, customer.getUser().getUserId())) {
                    throw new DuplicateEmailException(email);
                }
                customer.getUser().setEmail(email);
            }
        }
        return customerMapper.toProfileResponse(customerRepository.save(customer));
    }
}
