package com.fooddelivery.service;

import com.fooddelivery.entity.DishCategory;
import com.fooddelivery.repository.DishCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 家庭菜品分类「获取或创建」组件。
 *
 * 复制系统菜谱到家庭时，需要把系统分类映射为家庭分类：目标家庭存在同名分类则复用，
 * 否则自动创建。为了让并发创建时的唯一约束冲突只回滚这个独立事务、不污染外层复制事务，
 * 这里使用 REQUIRES_NEW 独立事务，并以数据库唯一约束 {@code uk_dish_categories_family_name}
 * 作为最终安全网。
 */
@Service
@RequiredArgsConstructor
public class FamilyDishCategoryResolver {

    private final DishCategoryRepository dishCategoryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DishCategory getOrCreate(Long familyId, String name) {
        String trimmed = name.trim();
        return dishCategoryRepository.findByFamilyIdAndName(familyId, trimmed)
                .orElseGet(() -> insertAndHandleConflict(familyId, trimmed));
    }

    private DishCategory insertAndHandleConflict(Long familyId, String name) {
        try {
            DishCategory category = DishCategory.builder()
                    .familyId(familyId)
                    .name(name)
                    .sortNo(0)
                    .build();
            return dishCategoryRepository.saveAndFlush(category);
        } catch (DataIntegrityViolationException e) {
            // 并发下撞唯一约束：重查已被并发事务提交的同名分类并复用
            return dishCategoryRepository.findByFamilyIdAndName(familyId, name)
                    .orElseThrow(() -> e);
        }
    }
}
