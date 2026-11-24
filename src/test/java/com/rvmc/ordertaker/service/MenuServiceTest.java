package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.MenuItemRequest;
import com.rvmc.ordertaker.dto.MenuItemResponse;
import com.rvmc.ordertaker.entity.MenuItem;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.exception.ResourceAlreadyTaggedAsDeletedException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.exception.ForbiddenException;
import com.rvmc.ordertaker.mapper.MenuItemMapper;
import com.rvmc.ordertaker.repository.MenuItemRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {
    @Mock private MenuItemMapper menuItemMapper;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @InjectMocks private MenuService menuService;

    // -------------------------------------------------------------------------
    // createMenuItem
    // -------------------------------------------------------------------------

    @Test void createMenuItem_shouldReturnSavedResponse_whenRestaurantExists() {
        // Arrange
        Long restaurantId = 1L;
        Long menuItemId = 10L;
        MenuItemRequest request = new MenuItemRequest(
                restaurantId,
                "Cheeseburger",
                "Juicy beef patty with cheese",
                BigDecimal.valueOf(250.0),
                true,
                false
        );

        Restaurant mockRestaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .restaurantName("Burger Joint")
                .build();

        MenuItem unmappedMenuItem = new MenuItem();
        MenuItem savedMenuItem = new MenuItem();

        MenuItemResponse expectedResponse = new MenuItemResponse(
                menuItemId,
                restaurantId,
                "Cheeseburger",
                "Juicy beef patty with cheese",
                BigDecimal.valueOf(250.0),
                true,
                false
        );

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(mockRestaurant));
        when(menuItemMapper.fromRequestToEntity(request)).thenReturn(unmappedMenuItem);
        when(menuItemRepository.save(unmappedMenuItem)).thenReturn(savedMenuItem);
        when(menuItemMapper.toResponse(savedMenuItem)).thenReturn(expectedResponse);

        // Act
        MenuItemResponse result = menuService.createMenuItem(request);

        // Assert
        assertNotNull(result);
        assertEquals("Cheeseburger", result.itemName());
        assertEquals(BigDecimal.valueOf(250.0), result.price());

        // Verify interactions
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(menuItemMapper, times(1)).fromRequestToEntity(request);
        verify(menuItemRepository, times(1)).save(unmappedMenuItem);
        verify(menuItemMapper, times(1)).toResponse(savedMenuItem);
    }

    @Test void createMenuItem_shouldThrowResourceNotFoundException_whenRestaurantDoesNotExist() {
        // Arrange
        Long restaurantId = 99L;
        MenuItemRequest request = new MenuItemRequest(
                restaurantId,
                "Cheeseburger",
                "Juicy beef patty with cheese",
                BigDecimal.valueOf(250.0),
                true,
                false
        );

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> menuService.createMenuItem(request)
        );

        assertNotNull(exception);

        // Verify repository was checked, but mapping and saving never happened
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(menuItemMapper, never()).fromRequestToEntity(any());
        verify(menuItemRepository, never()).save(any());
        verify(menuItemMapper, never()).toResponse(any());
    }

    // -------------------------------------------------------------------------
    // updateMenuItem
    // -------------------------------------------------------------------------

    @Test void updateMenuItem_shouldUpdateAndReturnResponse_whenItemExists() {
        // Arrange
        Long menuItemId = 1L;
        MenuItemRequest request = new MenuItemRequest(
                1L,
                "Double Cheeseburger",
                "Updated description",
                BigDecimal.valueOf(300.0),
                true,
                false
        );

        MenuItem existingItem = MenuItem.builder()
                .menuItemId(menuItemId)
                .itemName("Cheeseburger")
                .description("Juicy beef patty")
                .price(BigDecimal.valueOf(250.0))
                .build();

        MenuItem savedItem = MenuItem.builder()
                .menuItemId(menuItemId)
                .itemName("Double Cheeseburger")
                .description("Updated description")
                .price(BigDecimal.valueOf(300.0))
                .build();

        MenuItemResponse expectedResponse = new MenuItemResponse(
                menuItemId,
                1L,
                "Double Cheeseburger",
                "Updated description",
                BigDecimal.valueOf(300.0),
                true,
                false
        );

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(existingItem));
        // MapStruct updateEntityFromRequest is void, so we don't need to return anything for it
        doNothing().when(menuItemMapper).updateEntityFromRequest(request, existingItem);
        when(menuItemRepository.save(existingItem)).thenReturn(savedItem);
        when(menuItemMapper.toResponse(savedItem)).thenReturn(expectedResponse);

        // Act
        MenuItemResponse result = menuService.updateMenuItem(menuItemId, request);

        // Assert
        assertNotNull(result);
        assertEquals("Double Cheeseburger", result.itemName());
        assertEquals(BigDecimal.valueOf(300.0), result.price());

        // Verify interactions
        verify(menuItemRepository, times(1)).findById(menuItemId);
        verify(menuItemMapper, times(1)).updateEntityFromRequest(request, existingItem);
        verify(menuItemRepository, times(1)).save(existingItem);
        verify(menuItemMapper, times(1)).toResponse(savedItem);
    }

    @Test void updateMenuItem_shouldThrowResourceNotFoundException_whenItemDoesNotExist() {
        // Arrange
        Long menuItemId = 99L;
        MenuItemRequest request = new MenuItemRequest(
                1L,
                "Double Cheeseburger",
                "Updated description",
                BigDecimal.valueOf(300.0),
                true,
                false
        );

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> menuService.updateMenuItem(menuItemId, request)
        );

        assertNotNull(exception);

        // Verify repository lookup happened, but update/save/mapping never occurred
        verify(menuItemRepository, times(1)).findById(menuItemId);
        verify(menuItemMapper, never()).updateEntityFromRequest(any(), any());
        verify(menuItemRepository, never()).save(any());
        verify(menuItemMapper, never()).toResponse(any());
    }

    // -------------------------------------------------------------------------
    // updateItemAvailability
    // -------------------------------------------------------------------------

    @Test void updateItemAvailability_shouldToggleAvailabilityAndReturnResponse_whenItemExists() {
        // Arrange
        Long menuItemId = 1L;
        Restaurant mockRestaurant = Restaurant.builder().restaurantId(10L).build();

        // Initial state: isAvailable is true, we expect it to flip to false
        MenuItem existingItem = MenuItem.builder()
                .menuItemId(menuItemId)
                .restaurant(mockRestaurant)
                .itemName("Cheeseburger")
                .price(BigDecimal.valueOf(250.0))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        MenuItem savedItem = MenuItem.builder()
                .menuItemId(menuItemId)
                .restaurant(mockRestaurant)
                .itemName("Cheeseburger")
                .price(BigDecimal.valueOf(250.0))
                .isAvailable(false) // Flipped
                .isDeleted(false)
                .build();

        MenuItemResponse expectedResponse = new MenuItemResponse(
                menuItemId, 10L, "Cheeseburger", "Juicy beef patty", BigDecimal.valueOf(250.0), false, false
        );

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(existingItem));
        when(menuItemRepository.save(existingItem)).thenReturn(savedItem);
        when(menuItemMapper.toResponse(savedItem)).thenReturn(expectedResponse);

        // Act
        MenuItemResponse result = menuService.updateItemAvailability(menuItemId);

        // Assert
        assertNotNull(result);
        assertFalse(result.isAvailable()); // Verify availability flipped in response

        // Verify interactions
        verify(menuItemRepository, times(1)).findById(menuItemId);
        verify(menuItemRepository, times(1)).save(existingItem);
        verify(menuItemMapper, times(1)).toResponse(savedItem);
    }

    @Test void updateItemAvailability_shouldThrowResourceNotFoundException_whenItemDoesNotExist() {
        // Arrange
        Long menuItemId = 99L;
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> menuService.updateItemAvailability(menuItemId)
        );

        assertNotNull(exception);

        // Verify repository lookup happened, but save and mapping never occurred
        verify(menuItemRepository, times(1)).findById(menuItemId);
        verify(menuItemRepository, never()).save(any());
        verify(menuItemMapper, never()).toResponse(any());
    }

    // -------------------------------------------------------------------------
    // deleteMenuItemSoft
    // -------------------------------------------------------------------------

    @Test void deleteMenuItemSoft_shouldMarkDeleteAndReturnOk_whenItemExistsAndNotDeleted() {
//        Arrange
        Long menuItemId = 1L;
        MenuItem existingItem = MenuItem.builder()
                .menuItemId(menuItemId)
                .itemName("Margherita Pizza")
                .price(BigDecimal.valueOf(250.0))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(existingItem));
        when(menuItemRepository.save(existingItem)).thenReturn(existingItem);

//        Act
        menuService.deleteMenuItemSoft(menuItemId);

//        Assert
        assertTrue(existingItem.getIsDeleted());

//        Verify
        verify(menuItemRepository, times(1)).findById(menuItemId);
        verify(menuItemRepository, times(1)).save(existingItem);
    }

    @Test void deleteMenuItemSoft_shouldThrowResourceAlreadyTaggedAsDeletedException_whenItemAlreadyDeleted() {
//        Arrange
        Long menuItemId = 1L;
        MenuItem alreadyDeletedItem = MenuItem.builder()
                .menuItemId(menuItemId)
                .itemName("Margherita Pizza")
                .price(BigDecimal.valueOf(250.0))
                .isAvailable(true)
                .isDeleted(true)
                .build();

        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(alreadyDeletedItem));

//        Act
        ResourceAlreadyTaggedAsDeletedException exception = assertThrows(
                ResourceAlreadyTaggedAsDeletedException.class,
                () -> menuService.deleteMenuItemSoft(menuItemId)
        );

//        Assert
        assertEquals("Menu item with id " + menuItemId + " is already deleted.",
                exception.getMessage());

//        Verify
        verify(menuItemRepository, times(1)).findById(menuItemId);
        verify(menuItemRepository, never()).save(any());

    }

    @Test void deleteMenuItemSoft_shouldThrowResourceNotFoundException_whenItemDoesNotExist() {
//        Arrange
        Long menuItemId = 99L;
        when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.empty());

//        Act and Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> menuService.deleteMenuItemSoft(menuItemId)
        );

//        Verity
        verify(menuItemRepository, times(1)).findById(menuItemId);
        verify(menuItemRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // getMenuItemById
    // -------------------------------------------------------------------------

    @Test void getMenuItemById_shouldReturnMappedResponse_whenMenuItemExistsAndNotTaggedAsDeleted() {
        // Arrange
        Long menuItemId = 1L;
        Restaurant mockRestaurant = Restaurant.builder()
                .restaurantId(1L)
                .build();

        MenuItem mockMenuItem = MenuItem.builder()
                .menuItemId(menuItemId)
                .restaurant(mockRestaurant)
                .itemName("Cheeseburger")
                .description("Juicy beef patty with cheese")
                .price(BigDecimal.valueOf(250.0))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        MenuItemResponse expectedResponse = new MenuItemResponse(
                1L,
                1L,
                "Cheeseburger",
                "Juicy beef patty with cheese",
                BigDecimal.valueOf(250.0),
                true,
                false
        );

        when(menuItemRepository.findByMenuItemIdAndIsDeleted(menuItemId, false))
                .thenReturn(Optional.of(mockMenuItem));
        when(menuItemMapper.toResponse(mockMenuItem))
                .thenReturn(expectedResponse);

        // Act
        MenuItemResponse result = menuService.getMenuItemById(menuItemId);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.menuItemId());
        assertEquals(1L, result.restaurantId());
        assertEquals("Cheeseburger", result.itemName());
        assertFalse(result.isDeleted());

        // Verify
        verify(menuItemRepository, times(1)).findByMenuItemIdAndIsDeleted(menuItemId, false);
        verify(menuItemMapper, times(1)).toResponse(mockMenuItem);
    }

    @Test void getMenuItemById_shouldThrowResourceNotFoundException_whenItemDoesNotExistOrIsDeleted() {
        // Arrange
        Long menuItemId = 99L;
        when(menuItemRepository.findByMenuItemIdAndIsDeleted(menuItemId, false))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> menuService.getMenuItemById(menuItemId)
        );

        // Verify
        verify(menuItemRepository, times(1)).findByMenuItemIdAndIsDeleted(menuItemId, false);
        verify(menuItemMapper, never()).toResponse(any());
    }

    // -------------------------------------------------------------------------
    // getMenuFromRestaurant
    // -------------------------------------------------------------------------

    @Test void getMenuFromRestaurant_shouldReturnMappedMenuItems_whenRestaurantExistsAndHasActiveItems() {
        // Arrange
        Long restaurantId = 1L;
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .restaurantName("Burger Joint")
                .build();

        MenuItem item1 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .itemName("Cheeseburger")
                .description("Juicy beef patty with cheese")
                .price(BigDecimal.valueOf(250.0))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        MenuItem item2 = MenuItem.builder()
                .menuItemId(11L)
                .restaurant(restaurant)
                .itemName("Fries")
                .description("Crispy salted fries")
                .price(BigDecimal.valueOf(120.0))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        MenuItemResponse response1 = new MenuItemResponse(
                10L, restaurantId, "Cheeseburger", "Juicy beef patty with cheese",
                BigDecimal.valueOf(250.0), true, false
        );
        MenuItemResponse response2 = new MenuItemResponse(
                11L, restaurantId, "Fries", "Crispy salted fries",
                BigDecimal.valueOf(120.0), true, false
        );

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantAndIsDeleted(restaurant, false))
                .thenReturn(List.of(item1, item2));
        when(menuItemMapper.toResponse(item1)).thenReturn(response1);
        when(menuItemMapper.toResponse(item2)).thenReturn(response2);

        // Act
        List<MenuItemResponse> result = menuService.getMenuFromRestaurant(restaurantId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Cheeseburger", result.get(0).itemName());
        assertEquals("Fries", result.get(1).itemName());

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(menuItemRepository, times(1)).findByRestaurantAndIsDeleted(restaurant, false);
        verify(menuItemMapper, times(1)).toResponse(item1);
        verify(menuItemMapper, times(1)).toResponse(item2);
    }

    @Test void getMenuFromRestaurant_shouldReturnEmptyList_whenRestaurantExistsWithNoActiveItems() {
        // Arrange
        Long restaurantId = 1L;
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .restaurantName("Burger Joint")
                .build();

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuItemRepository.findByRestaurantAndIsDeleted(restaurant, false))
                .thenReturn(List.of());

        // Act
        List<MenuItemResponse> result = menuService.getMenuFromRestaurant(restaurantId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(menuItemRepository, times(1)).findByRestaurantAndIsDeleted(restaurant, false);
        verify(menuItemMapper, never()).toResponse(any());
    }

    @Test void getMenuFromRestaurant_shouldThrowResourceNotFoundException_whenRestaurantDoesNotExist() {
        // Arrange
        Long restaurantId = 99L;
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> menuService.getMenuFromRestaurant(restaurantId)
        );

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(menuItemRepository, never()).findByRestaurantAndIsDeleted(any(), anyBoolean());
        verify(menuItemMapper, never()).toResponse(any());
    }

    @Test
    void updateMenuItemForRestaurant_shouldRejectItemOwnedByAnotherRestaurant() {
        Restaurant otherRestaurant = Restaurant.builder().restaurantId(20L).build();
        MenuItem item = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(otherRestaurant)
                .build();
        MenuItemRequest request = new MenuItemRequest(
                10L, "Changed", null, BigDecimal.valueOf(250), true, false
        );
        when(menuItemRepository.findById(10L)).thenReturn(Optional.of(item));

        assertThrows(ForbiddenException.class,
                () -> menuService.updateMenuItemForRestaurant(10L, 1L, request));
        verify(menuItemMapper, never()).updateEntityFromRequest(any(), any());
        verify(menuItemRepository, never()).save(any());
    }
}