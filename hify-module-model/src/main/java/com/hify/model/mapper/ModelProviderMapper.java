package com.hify.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.model.entity.ModelProvider;
import com.hify.model.entity.ModelProviderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 模型提供商 Mapper
 *
 * @author hify
 */
@Mapper
public interface ModelProviderMapper extends BaseMapper<ModelProvider> {

    /**
     * 分页查询模型提供商（含健康状态）
     */
    Page<ModelProviderVO> selectProviderPage(Page<ModelProviderVO> page, @Param("deleted") Boolean deleted);

    /**
     * 查询模型提供商详情（含健康状态）
     */
    ModelProviderVO selectProviderDetail(@Param("id") Long id);
}
