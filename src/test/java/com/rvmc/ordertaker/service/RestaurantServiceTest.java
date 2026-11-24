package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.dto.RestaurantProfileRequest;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.User;
import com.rvmc.ordertaker.exception.DuplicateEmailException;
import com.rvmc.ordertaker.exception.ResourceAlreadyTaggedAsDeletedException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.RestaurantMapper;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import com.rvmc.ordertaker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private RestaurantMapper restaurantMapper;
    @Mock private UserRepository userRepository;
    @InjectMocks private RestaurantService restaurantService;

    // -------------------------------------------------------------------------
    // getRestaurantByQuery
    // -------------------------------------------------------------------------

    @Test void getRestaurantByQuery_shouldReturnMappedDto_whenMatchingRestaurantsExist() {
//        Arrange
        String query = "Italian";
        Restaurant mockRestaurant = Restaurant.builder()
                .restaurantId(1L)
                .restaurantName("Italian Bistro")
                .address1("123 Main St.")
                .address2("New York")
                .owner(new User())
                .isActive(true)
                .toDelete(false)
                .updatedAt(ZonedDateTime.now())
                .build();

        RestaurantProfileResponse mockResponse = new RestaurantProfileResponse(
                1L, "Italian Bistro", "123 Main St.", "New York", Collections.emptyList()
        );

        when(restaurantRepository.findAllByRestaurantNameContainingIgnoreCase("Italian"))
                .thenReturn(List.of(mockRestaurant));

        when(restaurantMapper.toRestaurantProfileResponse(mockRestaurant))
                .thenReturn(mockResponse);

//        Act
        List<RestaurantProfileResponse> result = restaurantService.getRestaurantByQuery(query);

//        Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.getFirst().restaurantId());
        assertEquals("Italian Bistro", result.getFirst().restaurantName());
        assertEquals("123 Main St.", result.getFirst().address1());
        assertEquals("New York", result.getFirst().address2());
        assertTrue(result.getFirst().menuItemList().isEmpty());

//        Verify repository interaction
        verify(restaurantRepository, times(1)).findAllByRestaurantNameContainingIgnoreCase("Italian");
        verify(restaurantMapper, times(1)).toRestaurantProfileResponse(mockRestaurant);
    }

    @Test void getRestaurantByQuery_shouldReturnEmptyList_whenNoRestaurantsMatch() {
//        Arrange
        String query = "NonExistent";

        when(restaurantRepository.findAllByRestaurantNameContainingIgnoreCase("NonExistent"))
                .thenReturn(Collections.emptyList());

//        Act
        List<RestaurantProfileResponse> result = restaurantService.getRestaurantByQuery(query);

//        Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

//        Verify interaction
        verify(restaurantRepository, times(1)).findAllByRestaurantNameContainingIgnoreCase("NonExistent");
    }

    // -------------------------------------------------------------------------
    // getAllRestaurants
    // -------------------------------------------------------------------------

    @Test void getAllRestaurants_shouldReturnMappedDto_whenRestaurantsExist() {
//        Arrange
        List<Restaurant> mockRestaurants = new ArrayList<>();
        mockRestaurants.add(Restaurant.builder()
                .restaurantId(1L)
                .restaurantName("Italian Bistro")
                .address1("123 Main St.")
                .address2("New York")
                .owner(new User())
                .isActive(true)
                .toDelete(false)
                .updatedAt(ZonedDateTime.now())
                .build());

        mockRestaurants.add(Restaurant.builder()
                .restaurantId(2L)
                .restaurantName("Cafe Manila")
                .address1("123 Taft Ave.")
                .address2("Manila")
                .owner(new User())
                .isActive(true)
                .toDelete(false)
                .updatedAt(ZonedDateTime.now())
                .build());

        List<RestaurantProfileResponse> mockResponses = List.of(
                new RestaurantProfileResponse(1L, "Italian Bistro", "123 Main St.", "New York", Collections.emptyList()),
                new RestaurantProfileResponse(2L, "Cafe Manila", "123 Taft Ave.", "Manila", Collections.emptyList())
        );

        when(restaurantRepository.findAll()).thenReturn(mockRestaurants);
        when(restaurantMapper.toRestaurantProfileResponse(mockRestaurants.get(0))).thenReturn(mockResponses.get(0));
        when(restaurantMapper.toRestaurantProfileResponse(mockRestaurants.get(1))).thenReturn(mockResponses.get(1));

//        Act
        List<RestaurantProfileResponse> result = restaurantService.getAllRestaurants();

//        Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(1L, result.getFirst().restaurantId());
        assertEquals("Italian Bistro", result.getFirst().restaurantName());

        assertEquals(2L, result.getLast().restaurantId());
        assertEquals("Cafe Manila", result.getLast().restaurantName());

//        Verify
        verify(restaurantRepository, times(1)).findAll();
        verify(restaurantMapper, times(1)).toRestaurantProfileResponse(mockRestaurants.getFirst());
        verify(restaurantMapper, times(1)).toRestaurantProfileResponse(mockRestaurants.getLast());

    }

    @Test void getAllRestaurants_shouldReturnEmptyList_whenNoRestaurantsExist() {
        // Arrange
        when(restaurantRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<RestaurantProfileResponse> result = restaurantService.getAllRestaurants();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify
        verify(restaurantRepository, times(1)).findAll();
        // Ensure the mapper was never called since the list was empty
        verify(restaurantMapper, never()).toRestaurantProfileResponse(any());
    }

    // -------------------------------------------------------------------------
    // getRestaurantById
    // -------------------------------------------------------------------------

    @Test void getRestaurantById_shouldReturnMappedDto_whenRestaurantExist() {
//        Arrange
        Long restaurantId = 1L;
        Restaurant mockRestaurant = Restaurant.builder()
                .restaurantId(1L)
                .restaurantName("Italian Bistro")
                .address1("123 Main St.")
                .address2("New York")
                .owner(new User())
                .isActive(true)
                .toDelete(false)
                .updatedAt(ZonedDateTime.now())
                .build();

        RestaurantProfileResponse mockResponse = new RestaurantProfileResponse(
                1L, "Italian Bistro", "123 Main St.", "New York", Collections.emptyList()
        );

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(mockRestaurant));
        when(restaurantMapper.toRestaurantProfileResponse(mockRestaurant)).thenReturn(mockResponse);

//        Act
        RestaurantProfileResponse result = restaurantService.getRestaurantById(restaurantId);

//        Assert
        assertNotNull(result);
        assertEquals(1L, result.restaurantId());
        assertEquals("Italian Bistro", result.restaurantName());
        assertEquals("123 Main St.", result.address1());
        assertEquals("New York", result.address2());
        assertTrue(result.menuItemList().isEmpty());

//        Verify
        verify(restaurantRepository, times(1)).findById(1L);
        verify(restaurantMapper, times(1)).toRestaurantProfileResponse(mockRestaurant);
    }

    @Test void getRestaurantById_shouldThrowResourceNotFoundException_whenRestaurantDoesNotExist() {
//        Arrange
        Long restaurantId = 99L;

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

//        Act and assert throw
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> restaurantService.getRestaurantById(restaurantId)
        );

//        Assert
        assertEquals("Restaurant not found with id: 99", exception.getMessage());

//        Validate
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(restaurantMapper, never()).toRestaurantProfileResponse(any());
    }

    @Test
    void editProfile_shouldUpdateProfileAndNormalizeEmail() {
        User owner = User.builder().userId(2L).email("old@example.com").build();
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(1L)
                .restaurantName("Old Name")
                .address1("Old Address")
                .address2("Old City")
                .owner(owner)
                .isActive(true)
                .toDelete(false)
                .build();
        RestaurantProfileRequest request = new RestaurantProfileRequest(
                "new-owner", " New.Owner@Example.com ", "New Name", null, "New City"
        );
        RestaurantProfileResponse response = new RestaurantProfileResponse(
                1L, "New Name", "Old Address", "New City", Collections.emptyList()
        );

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(userRepository.existsByEmailAndUserIdNot("new.owner@example.com", 2L))
                .thenReturn(false);
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(restaurantMapper.toRestaurantProfileResponse(restaurant)).thenReturn(response);

        assertEquals(response, restaurantService.editProfile(1L, request));
        assertEquals("New Name", restaurant.getRestaurantName());
        assertEquals("New City", restaurant.getAddress2());
        assertEquals("new-owner", owner.getUsername());
        assertEquals("new.owner@example.com", owner.getEmail());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void editProfile_shouldRejectEmailOwnedByAnotherUser() {
        User owner = User.builder().userId(2L).email("old@example.com").build();
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(1L)
                .owner(owner)
                .isActive(true)
                .toDelete(false)
                .build();
        RestaurantProfileRequest request = new RestaurantProfileRequest(
                null, "taken@example.com", null, null, null
        );

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(userRepository.existsByEmailAndUserIdNot("taken@example.com", 2L))
                .thenReturn(true);

        assertThrows(
                DuplicateEmailException.class,
                () -> restaurantService.editProfile(1L, request)
        );
    }

    @Test
    void deleteProfile_shouldDeactivateAndSoftDeleteRestaurant() {
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(1L)
                .isActive(true)
                .toDelete(false)
                .build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        restaurantService.deleteProfile(1L);

        assertTrue(restaurant.getToDelete());
        assertFalse(restaurant.getIsActive());
        verify(restaurantRepository).save(restaurant);
    }

    @Test
    void deleteProfile_shouldRejectAlreadyDeletedRestaurant() {
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(1L)
                .isActive(false)
                .toDelete(true)
                .build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        assertThrows(
                ResourceAlreadyTaggedAsDeletedException.class,
                () -> restaurantService.deleteProfile(1L)
        );
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    void hardDeleteRestaurantByAdmin_shouldDeleteOwnerAccount() {
        User owner = User.builder().userId(2L).build();
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(1L)
                .owner(owner)
                .build();
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        restaurantService.hardDeleteRestaurantByAdmin(1L);

        verify(userRepository).delete(owner);
        verify(restaurantRepository, never()).delete(any());
    }

    @Test
    void reactivateRestaurantByAdmin_shouldReverseSoftDeletionFlags() {
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(1L)
                .isActive(false)
                .toDelete(true)
                .build();
        RestaurantProfileResponse response = new RestaurantProfileResponse(
                1L, "Bistro", "Address", "City", Collections.emptyList()
        );
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);
        when(restaurantMapper.toRestaurantProfileResponse(restaurant)).thenReturn(response);

        assertEquals(response, restaurantService.reactivateRestaurantByAdmin(1L));
        assertTrue(restaurant.getIsActive());
        assertFalse(restaurant.getToDelete());
        verify(restaurantRepository).save(restaurant);
    }
}
