package com.fooddelivery.service;

import com.fooddelivery.dto.AddShoppingItemRequest;
import com.fooddelivery.dto.ShoppingItemResponse;

import java.util.List;

public interface ShoppingListService {

    List<ShoppingItemResponse> listItems(Long familyId);

    ShoppingItemResponse addItem(Long familyId, AddShoppingItemRequest request);
}