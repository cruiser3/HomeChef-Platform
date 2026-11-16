package com.fooddelivery.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "dietary_preferences", length = 200)
    private String dietaryPreferences;

    @Column(name = "dietary_restrictions", length = 200)
    private String dietaryRestrictions;

    @Column(name = "allergies", length = 200)
    private String allergies;

    @Column(name = "medical_conditions", length = 200)
    private String medicalConditions;

    @Enumerated(EnumType.STRING)
    @Column(name = "`role`", nullable = false, length = 32)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public enum Role {
        CUSTOMER, RESTAURANT_OWNER, ADMIN
    }

    /**
     * 饮食偏好标签（白名单）。仅允许这些预定义值，天然排除疾病、用药等敏感健康信息。
     */
    public enum DietaryPreference {
        FITNESS, FAT_LOSS, MUSCLE_GAIN, LIGHT_DIET, VEGETARIAN
    }

    /**
     * 忌口标签预置项（前端快捷选项）。用户可在此基础上自定义其他忌口文本，后端不再强制限定枚举。
     */
    public enum DietaryRestriction {
        NO_SPICY, NO_CILANTRO, NO_SEAFOOD, NO_BEEF_MUTTON, NO_PORK, NO_ALLIUM
    }

    /**
     * 过敏原标签预置项（前端快捷选项）。用户可在此基础上自定义其他过敏文本。
     */
    public enum Allergy {
        SEAFOOD, PEANUT, TREE_NUT, MILK, EGG, SOY, WHEAT, SESAME
    }

    /**
     * 疾病标签预置项（前端快捷选项）。用户可在此基础上自定义其他疾病文本。
     * 属敏感健康信息，仅用于饮食参考，不构成医疗建议。
     */
    public enum MedicalCondition {
        DIABETES, HYPERTENSION, HYPERLIPIDEMIA, GOUT,
        KIDNEY_DISEASE, FATTY_LIVER, CORONARY_HEART_DISEASE, GASTRITIS
    }
}