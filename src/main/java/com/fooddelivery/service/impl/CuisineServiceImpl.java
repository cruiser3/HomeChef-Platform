package com.fooddelivery.service.impl;

import com.fooddelivery.dto.CuisineRequest;
import com.fooddelivery.dto.CuisineResponse;
import com.fooddelivery.entity.Cuisine;
import com.fooddelivery.repository.CuisineRepository;
import com.fooddelivery.service.CuisineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CuisineServiceImpl implements CuisineService {

    private final CuisineRepository cuisineRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CuisineResponse> list() {
        return cuisineRepository.findByAvailableTrueOrderBySortNoAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CuisineResponse create(CuisineRequest request) {
        String name = request.getName().trim();
        return cuisineRepository.findByName(name)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(cuisineRepository.save(
                        Cuisine.builder()
                                .name(name)
                                .sortNo(nextSortNo())
                                .available(true)
                                .build())));
    }

    private int nextSortNo() {
        return cuisineRepository.findTopBySortNoLessThanOrderBySortNoDesc(Cuisine.OTHER_SORT_NO)
                .map(c -> c.getSortNo() + 1)
                .orElse(1);
    }

    private CuisineResponse toResponse(Cuisine c) {
        return CuisineResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .sortNo(c.getSortNo())
                .build();
    }
}
