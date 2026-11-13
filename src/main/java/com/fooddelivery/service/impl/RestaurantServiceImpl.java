package com.fooddelivery.service.impl;

import com.fooddelivery.dto.RestaurantRequest;
import com.fooddelivery.dto.RestaurantResponse;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.exception.ForbiddenException;
import com.fooddelivery.exception.ResourceNotFoundException;
import com.fooddelivery.repository.RestaurantRepository;
import com.fooddelivery.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Override
    public RestaurantResponse create(RestaurantRequest request) {
        // 旧 Restaurant 模块写接口暂时默认拒绝（403）。
        // 原因：Restaurant.ownerId 由客户端任意传入且无归属校验，系统当前没有可信的餐厅
        // 所有权授权体系，直接开放会造成越权与数据完整性风险。
        // 未来恢复需先建立可信的餐厅所有权授权体系（ownerId 改为对 User 的真实外键、由认证
        // 上下文写入且禁止客户端篡改，并配套餐厅 owner 角色授权规则）。
        throw new ForbiddenException("Restaurant creation is temporarily disabled");
    }

    @Override
    public RestaurantResponse getById(Long id) {
        Restaurant restaurant = findByIdOrThrow(id);
        return toResponse(restaurant);
    }

    @Override
    public List<RestaurantResponse> getAll() {
        return restaurantRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public RestaurantResponse update(Long id, RestaurantRequest request) {
        // 旧 Restaurant 模块写接口暂时默认拒绝（403）。
        // 原因：无归属校验，任意登录用户可修改任意餐厅，且可通过篡改 ownerId 劫持餐厅所有权；
        // 系统当前没有可信的餐厅所有权授权体系。
        throw new ForbiddenException("Restaurant update is temporarily disabled");
    }

    @Override
    public void delete(Long id) {
        // 旧 Restaurant 模块写接口暂时默认拒绝（403）。
        // 原因：无归属校验，任意登录用户可删除任意餐厅并级联删除其菜单；
        // 系统当前没有可信的餐厅所有权授权体系。
        throw new ForbiddenException("Restaurant deletion is temporarily disabled");
    }

    private Restaurant findByIdOrThrow(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }

    private RestaurantResponse toResponse(Restaurant restaurant) {
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .ownerId(restaurant.getOwnerId())
                .active(restaurant.isActive())
                .menuItemCount(restaurant.getMenuItems() == null ? 0 : restaurant.getMenuItems().size())
                .build();
    }
}