package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 多模态消息内容片段
 * <p>支持 text 和 image_url 两种类型</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmContentPart implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 类型: text / image_url
     */
    private String type;

    /**
     * 文本内容（type=text 时使用）
     */
    private String text;

    /**
     * 图片 URL 或 base64（type=image_url 时使用）
     */
    private ImageUrl imageUrl;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageUrl implements Serializable {
        private static final long serialVersionUID = 1L;
        private String url;
        private String detail;
    }
}
