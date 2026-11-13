package com.fooddelivery.service.impl;
import org.springframework.transaction.annotation.Transactional;
import com.fooddelivery.dto.MenuItemRequest;
import com.fooddelivery.dto.MenuItemResponse;
import com.fooddelivery.entity.MenuItem;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.MenuItemRepository;
import com.fooddelivery.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;

    @Override
    public MenuItemResponse create(MenuItemRequest request) {
        // 旧 MenuItem 模块写接口暂时默认拒绝（403）。
        // 原因：restaurantId 由客户端任意指定且无归属校验，任意登录用户可向他人餐厅注入菜单项；
        // 系统当前没有可信的餐厅所有权授权体系。
        throw new ForbiddenException("Menu item creation is temporarily disabled");
    }

    @Override
    public MenuItemResponse getById(Long id) {
        return toResponse(findByIdOrThrow(id));
    }

    @Override
    public List<MenuItemResponse> getByRestaurant(Long restaurantId) {
        return menuItemRepository.findByRestaurantId(restaurantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MenuItemResponse update(Long id, MenuItemRequest request) {
        // 旧 MenuItem 模块写接口暂时默认拒绝（403）。
        // 原因：无归属校验，任意登录用户可修改任意菜单项；
        // 系统当前没有可信的餐厅所有权授权体系。
        throw new ForbiddenException("Menu item update is temporarily disabled");
    }

    @Override
    public void delete(Long id) {
        // 旧 MenuItem 模块写接口暂时默认拒绝（403）。
        // 原因：无归属校验，任意登录用户可删除任意菜单项；
        // 系统当前没有可信的餐厅所有权授权体系。
        throw new ForbiddenException("Menu item deletion is temporarily disabled");
    }

    private MenuItem findByIdOrThrow(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .price(item.getPrice())
                .available(item.isAvailable())
                .restaurantId(item.getRestaurant().getId())
                .restaurantName(item.getRestaurant().getName())
                .build();
    }
}
