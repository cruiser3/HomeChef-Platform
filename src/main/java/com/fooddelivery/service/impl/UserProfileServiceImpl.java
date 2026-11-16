package com.fooddelivery.service.impl;

import com.fooddelivery.dto.UpdateUserProfileRequest;
import com.fooddelivery.dto.UserProfileResponse;
import com.fooddelivery.entity.User;
import com.fooddelivery.exception.BadRequestException;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.service.CurrentUserService;
import com.fooddelivery.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile() {
        return toResponse(currentUserService.getCurrentUser());
    }

    @Override
    public UserProfileResponse updateProfile(UpdateUserProfileRequest request) {
        User user = currentUserService.getCurrentUser();
        user.setName(request.getName().trim());
        user.setAvatarUrl(normalizeAvatar(request.getAvatarUrl()));
        user.setDietaryPreferences(serializeEnumList(
                request.getDietaryPreferences(), User.DietaryPreference.class, "dietary preference"));
        user.setDietaryRestrictions(serializeTagList(request.getDietaryRestrictions()));
        user.setAllergies(serializeTagList(request.getAllergies()));
        user.setMedicalConditions(serializeTagList(request.getMedicalConditions()));
        return toResponse(userRepository.save(user));
    }

    private String normalizeAvatar(String avatarUrl) {
        if (avatarUrl == null) {
            return null;
        }
        String trimmed = avatarUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 将前端传入的标签列表标准化为逗号分隔字符串，并校验每个值必须落在给定枚举白名单内。
     * 通过枚举白名单机制从源头排除疾病、用药、医疗过敏原等敏感健康信息。
     *
     * @param values   原始标签列表
     * @param enumType 目标白名单枚举（{@link Enum} 的 {@link Class}）
     * @param label    用于错误提示的语义名
     * @param <E>      枚举类型
     * @return 去重排序后的逗号分隔字符串；空列表返回 null
     */
    private <E extends Enum<E>> String serializeEnumList(List<String> values, Class<E> enumType, String label) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .toList();
        for (String value : normalized) {
            try {
                Enum.valueOf(enumType, value);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Unsupported " + label + ": " + value);
            }
        }
        return String.join(",", normalized);
    }

    /**
     * 将忌口 / 过敏标签列表标准化为逗号分隔字符串，允许用户自定义非枚举值。
     * 仅做 trim、去空、去重，并保留原始大小写（中文 / 自定义标签不被强制转为大写）。
     * 预置枚举项仍作为前端常用快捷选项，由前端映射中文文案；此处不再做枚举白名单强制校验。
     *
     * @param values 原始标签列表
     * @return 去重后的逗号分隔字符串；空列表返回 null
     */
    private String serializeTagList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> normalized = values.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private List<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private UserProfileResponse toResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .dietaryPreferences(parseCsv(user.getDietaryPreferences()))
                .dietaryRestrictions(parseCsv(user.getDietaryRestrictions()))
                .allergies(parseCsv(user.getAllergies()))
                .medicalConditions(parseCsv(user.getMedicalConditions()))
                .build();
    }
}
