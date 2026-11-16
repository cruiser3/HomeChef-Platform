package com.fooddelivery.service;

import com.fooddelivery.dto.AddShoppingItemRequest;
import com.fooddelivery.dto.ShoppingItemResponse;
import com.fooddelivery.dto.UpdatePurchasedRequest;
import com.fooddelivery.dto.UpdateShoppingItemRequest;

import java.util.List;

public interface ShoppingListService {

    List<ShoppingItemResponse> listItems(Long familyId);

    ShoppingItemResponse addItem(Long familyId, AddShoppingItemRequest request);

    ShoppingItemResponse updateItem(Long familyId, Long itemId, UpdateShoppingItemRequest request);

    ShoppingItemResponse markPurchased(Long familyId, Long itemId, UpdatePurchasedRequest request);

    void deleteItem(Long familyId, Long itemId);
}