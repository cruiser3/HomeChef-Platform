package com.fooddelivery.service;

import com.fooddelivery.dto.AddCartItemRequest;
import com.fooddelivery.dto.CartResponse;
import com.fooddelivery.dto.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCart(Long familyId);

    CartResponse addItem(Long familyId, AddCartItemRequest request);

    CartResponse updateItem(Long familyId, Long itemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long familyId, Long itemId);

    CartResponse clear(Long familyId);
}