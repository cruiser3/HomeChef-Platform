package com.fooddelivery.service;

import com.fooddelivery.dto.AssignResponsibleRequest;
import com.fooddelivery.dto.CreateFamilyOrderRequest;
import com.fooddelivery.dto.FamilyOrderResponse;
import com.fooddelivery.dto.UpdateCookStatusRequest;

import java.util.List;

public interface FamilyOrderService {

    FamilyOrderResponse createOrder(Long familyId, CreateFamilyOrderRequest request);

    List<FamilyOrderResponse> listOrders(Long familyId);

    FamilyOrderResponse getOrder(Long familyId, Long orderId);

    FamilyOrderResponse cancelOrder(Long familyId, Long orderId);

    FamilyOrderResponse startPreparing(Long familyId, Long orderId);

    FamilyOrderResponse markMealServed(Long familyId, Long orderId);

    void deleteOrder(Long familyId, Long orderId);

    FamilyOrderResponse updateCookStatus(Long familyId, Long orderItemId, UpdateCookStatusRequest request);

    FamilyOrderResponse assignResponsible(Long familyId, Long orderItemId, AssignResponsibleRequest request);
}