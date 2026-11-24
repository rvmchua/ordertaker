package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.CustomerProfileResponse;
import com.rvmc.ordertaker.dto.CustomerProfileRequest;
import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.User;
import com.rvmc.ordertaker.exception.DuplicateEmailException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.CustomerMapper;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock private CustomerRepository customerRepository;
    @Mock private UserRepository userRepository;
    @Mock private CustomerMapper customerMapper;
    @InjectMocks private CustomerService customerService;

    @Test
    void findByCustomerId_shouldReturnMappedProfile_whenCustomerExists() {
        Customer customer = Customer.builder()
                .customerId(1L)
                .customerName("Royce")
                .build();
        CustomerProfileResponse response = new CustomerProfileResponse(
                "royce", "royce@example.com", "Royce", "Address 1", "Address 2"
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerMapper.toProfileResponse(customer)).thenReturn(response);

        assertEquals(response, customerService.findByCustomerId(1L));
        verify(customerRepository).findById(1L);
        verify(customerMapper).toProfileResponse(customer);
    }

    @Test
    void findByCustomerId_shouldThrowNotFound_whenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> customerService.findByCustomerId(99L)
        );
        verifyNoInteractions(customerMapper);
    }

    @Test
    void editProfile_shouldNormalizeAndSaveEmail_whenEmailIsUnique() {
        User user = User.builder().userId(2L).email("old@example.com").build();
        Customer customer = Customer.builder().customerId(1L).user(user).build();
        CustomerProfileRequest request = new CustomerProfileRequest(
                null, "New.User@Example.com", null, null, null
        );
        CustomerProfileResponse response = new CustomerProfileResponse(
                null, "new.user@example.com", null, null, null
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(userRepository.existsByEmailAndUserIdNot("new.user@example.com", 2L))
                .thenReturn(false);
        when(customerRepository.save(customer)).thenReturn(customer);
        when(customerMapper.toProfileResponse(customer)).thenReturn(response);

        assertEquals(response, customerService.editProfile(1L, request));
        assertEquals("new.user@example.com", user.getEmail());
        verify(customerRepository).save(customer);
    }

    @Test
    void editProfile_shouldRejectEmailOwnedByAnotherUser() {
        User user = User.builder().userId(2L).email("old@example.com").build();
        Customer customer = Customer.builder().customerId(1L).user(user).build();
        CustomerProfileRequest request = new CustomerProfileRequest(
                null, "taken@example.com", null, null, null
        );

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(userRepository.existsByEmailAndUserIdNot("taken@example.com", 2L))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> customerService.editProfile(1L, request)
        );
    }
}
