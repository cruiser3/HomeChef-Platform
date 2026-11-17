package com.fooddelivery.service;

import com.fooddelivery.dto.CuisineRequest;
import com.fooddelivery.dto.CuisineResponse;

import java.util.List;

public interface CuisineService {

    List<CuisineResponse> list();

    CuisineResponse create(CuisineRequest request);
}
