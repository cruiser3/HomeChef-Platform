package com.fooddelivery.service.importing;

import com.fooddelivery.service.importing.HowToCookMarkdownParser.ParsedDish;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HowToCookMarkdownParserTest {

    private final HowToCookMarkdownParser parser = new HowToCookMarkdownParser();

    @Test
    @DisplayName("完整菜谱：标题/难度/卡路里/描述/食材/步骤数量正确")
    void parseFullDish() {
        String md = """
                # 西红柿炒鸡蛋的做法

                一道酸甜开胃的家常菜肴。

                预估烹饪难度：★★

                预估卡路里：252 大卡

                ## 必备原料和工具

                * 西红柿
                * 鸡蛋

                ## 计算

                总量：

                * 西红柿 = 1 个（约 180g） * 份数
                * 鸡蛋 = 1.5 个 * 份数，向上取整
                * 盐 = 1.5-2g * 份数
                * 食用油 = 4ml * 鸡蛋/个

                ## 操作

                1. 西红柿洗净
                2. 西红柿去蒂，切成小块
                3. 热锅，加入食用油

                ## 附加内容

                如果您遵循本指南的制作流程而发现有问题或可以改进的流程，请提出 Issue。
                """;

        ParsedDish d = parser.parse(md);

        assertEquals("西红柿炒鸡蛋", d.name);
        assertEquals(2, d.difficulty);
        assertEquals(252, d.calories);
        assertTrue(d.description.contains("酸甜开胃"));
        assertEquals(4, d.ingredients.size());
        assertEquals(3, d.steps.size());
    }

    @Test
    @DisplayName("简单用量：数字+单位正确提取 amount/unit")
    void parseSimpleAmount() {
        ParsedDish d = parser.parse("""
                # 示例的做法

                ## 计算

                * 咖喱块 115g
                * 盐: 2 g
                * 生抽: 15ml
                """);

        assertEquals(3, d.ingredients.size());

        assertEquals("咖喱块", d.ingredients.get(0).name);
        assertEquals(new BigDecimal("115"), d.ingredients.get(0).amount);
        assertEquals("g", d.ingredients.get(0).unit);
        assertNull(d.ingredients.get(0).amountText);

        assertEquals("盐", d.ingredients.get(1).name);
        assertEquals(new BigDecimal("2"), d.ingredients.get(1).amount);
        assertEquals("g", d.ingredients.get(1).unit);

        assertEquals("生抽", d.ingredients.get(2).name);
        assertEquals(new BigDecimal("15"), d.ingredients.get(2).amount);
        assertEquals("ml", d.ingredients.get(2).unit);
    }

    @Test
    @DisplayName("无法量化：区间/份数公式/多选/比例 → amount 置空且保留原文")
    void parseUncertainAmounts() {
        ParsedDish d = parser.parse("""
                # 示例的做法

                ## 计算

                * 糖: 5-10g
                * 西红柿 = 1 个（约 180g） * 份数
                * 干木耳: 20g / 湿木耳: 120g
                * 食用油 = 4ml * 鸡蛋/个
                * 芥末: （约 2cm）
                """);

        assertEquals(5, d.ingredients.size());
        for (HowToCookMarkdownParser.Ingredient ing : d.ingredients) {
            assertNull(ing.amount);
            assertNull(ing.unit);
            assertNotNull(ing.amountText);
        }
        assertTrue(d.ingredients.get(1).amountText.contains("份数"));
        assertTrue(d.ingredients.get(2).amountText.contains("/"));
    }

    @Test
    @DisplayName("可选标记：识别「可选」并清理名称")
    void parseOptionalIngredient() {
        ParsedDish d = parser.parse("""
                # 示例的做法

                ## 计算

                * 糖（可选）: 5g
                """);

        assertEquals(1, d.ingredients.size());
        assertEquals("糖", d.ingredients.get(0).name);
        assertTrue(d.ingredients.get(0).optional);
        assertEquals(new BigDecimal("5"), d.ingredients.get(0).amount);
    }

    @Test
    @DisplayName("操作步骤：子步骤合并进父步骤，序号按顺序重排")
    void parseStepsWithSubsteps() {
        ParsedDish d = parser.parse("""
                # 示例的做法

                ## 操作

                1. 第一步

                  - 子步骤A
                  - 子步骤B

                2. 第二步
                """);

        assertEquals(2, d.steps.size());
        assertEquals(1, d.steps.get(0).stepNo);
        assertEquals("第一步\n子步骤A\n子步骤B", d.steps.get(0).content);
        assertEquals(2, d.steps.get(1).stepNo);
        assertEquals("第二步", d.steps.get(1).content);
    }

    @Test
    @DisplayName("HTML 注释被剔除，不进入描述或步骤")
    void stripHtmlComments() {
        ParsedDish d = parser.parse("""
                # 示例的做法

                <!-- 这是一段注释 -->
                这是描述。

                ## 操作

                1. 步骤一 <!-- 行内注释 --> 完成
                """);

        assertTrue(d.description.contains("这是描述"));
        assertTrue(!d.description.contains("注释"));
        assertEquals("步骤一  完成", d.steps.get(0).content);
    }

    @Test
    @DisplayName("缺少标题：name 为 null")
    void parseWithoutTitle() {
        ParsedDish d = parser.parse("""
                这是一段没有标题的文本。

                预估烹饪难度：★★
                """);
        assertNull(d.name);
    }

    @Test
    @DisplayName("说明文字段落（含数字）不解析为食材，仅列表项被解析")
    void skipInstructionParagraphs() {
        ParsedDish d = parser.parse("""
                # 示例的做法

                ## 计算

                每次制作前需要确定计划做几份。一份正好够 1 个人吃。

                每 2 份：

                以下用量适合 3 至 5 人食用。

                * 鸡蛋 1 个
                * 沸水 100ml
                """);

        assertEquals(2, d.ingredients.size());
        assertEquals("鸡蛋", d.ingredients.get(0).name);
        assertEquals(new BigDecimal("1"), d.ingredients.get(0).amount);
        assertEquals("个", d.ingredients.get(0).unit);
        assertEquals("沸水", d.ingredients.get(1).name);
        assertEquals(new BigDecimal("100"), d.ingredients.get(1).amount);
        assertEquals("ml", d.ingredients.get(1).unit);
    }

    @Test
    @DisplayName("加号列表前缀的食材正确解析（name 不带 +）")
    void parsePlusListItem() {
        ParsedDish d = parser.parse("""
                # 示例的做法

                ## 计算

                + 猪肉 250g
                + 青椒 3 个
                """);

        assertEquals(2, d.ingredients.size());
        assertEquals("猪肉", d.ingredients.get(0).name);
        assertEquals(new BigDecimal("250"), d.ingredients.get(0).amount);
        assertEquals("g", d.ingredients.get(0).unit);
        assertEquals("青椒", d.ingredients.get(1).name);
        assertEquals(new BigDecimal("3"), d.ingredients.get(1).amount);
        assertEquals("个", d.ingredients.get(1).unit);
    }

    @Test
    @DisplayName("封面图：成品/预览图优先，否则第一张整行图片，无图则为 null")
    void parseCoverImage() {
        // 成品图优先（描述区的成品图）
        ParsedDish withProduct = parser.parse("""
                # 鸡蛋羹的做法

                描述。

                ![鸡蛋羹成品](./鸡蛋羹.jpg)

                ## 操作

                1. 步骤一
                """);
        assertEquals("./鸡蛋羹.jpg", withProduct.coverImage);

        // 预览图优先于前面的步骤图
        ParsedDish withPreview = parser.parse("""
                # 凉拌木耳的做法

                ## 操作

                ![干木耳](1.jpg)
                ![木耳](2.jpg)
                ![凉拌木耳-预览图-10](10.jpg)
                """);
        assertEquals("10.jpg", withPreview.coverImage);

        // 无成品/预览图时取第一张整行图片
        ParsedDish firstOnly = parser.parse("""
                # 示例的做法

                ## 操作

                ![图A](./a.jpg)
                ![图B](./b.jpg)
                """);
        assertEquals("./a.jpg", firstOnly.coverImage);

        // 图片带标题时只保留路径
        ParsedDish withTitle = parser.parse("""
                # 示例的做法

                ![示例成品](./a.jpg "标题")
                """);
        assertEquals("./a.jpg", withTitle.coverImage);

        // 无图片时 coverImage 为 null
        ParsedDish noImage = parser.parse("""
                # 示例的做法

                ## 操作

                1. 步骤一
                """);
        assertNull(noImage.coverImage);
    }
}
