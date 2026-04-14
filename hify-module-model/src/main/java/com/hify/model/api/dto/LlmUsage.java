package com.hify.model.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token 使用量
 *
 * @author hify
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmUsage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long promptTokens;
    private Long completionTokens;
    private Long totalTokens;
}
