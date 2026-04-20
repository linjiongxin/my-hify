package com.hify.model.service.impl;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.dto.ModelConfigDTO;
import com.hify.model.dto.ModelConfigCreateRequest;
import com.hify.model.dto.ModelConfigUpdateRequest;
import com.hify.model.entity.ModelConfig;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.hify.model.mapper.ModelConfigMapper;
import com.hify.model.vo.ModelConfigVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelConfigServiceImplTest {

    @Spy
    @InjectMocks
    private ModelConfigServiceImpl modelConfigService;

    @Mock
    private ModelConfigMapper modelConfigMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(modelConfigService, "baseMapper", modelConfigMapper);
    }

    @Test
    void shouldReturnVo_whenGetModelDetail_givenExistingId() {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setName("gpt-4o");
        model.setModelId("gpt-4o");
        model.setProviderId(1L);
        doReturn(model).when(modelConfigService).getById(1L);

        ModelConfigVO vo = modelConfigService.getModelDetail(1L);

        assertThat(vo).isNotNull();
        assertThat(vo.getName()).isEqualTo("gpt-4o");
        assertThat(vo.getModelId()).isEqualTo("gpt-4o");
    }

    @Test
    void shouldThrowBizException_whenGetModelDetail_givenNonExistingId() {
        doReturn(null).when(modelConfigService).getById(999L);

        assertThatThrownBy(() -> modelConfigService.getModelDetail(999L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldCreateModel_withoutClearingDefault_whenDefaultModelIsFalse() {
        ModelConfigCreateRequest request = new ModelConfigCreateRequest();
        request.setProviderId(1L);
        request.setName("claude-3");
        request.setModelId("claude-3-opus");
        request.setDefaultModel(false);

        doAnswer(inv -> {
            inv.getArgument(0, ModelConfig.class).setId(3L);
            return true;
        }).when(modelConfigService).save(any(ModelConfig.class));

        Long id = modelConfigService.createModel(request);

        assertThat(id).isEqualTo(3L);
        verify(modelConfigService, never()).list(any(Wrapper.class));
        verify(modelConfigService, never()).updateBatchById(anyList());
    }

    @Test
    void shouldCreateModel_andClearDefault_whenDefaultModelIsTrue() {
        ModelConfigCreateRequest request = new ModelConfigCreateRequest();
        request.setProviderId(1L);
        request.setName("gpt-4o");
        request.setModelId("gpt-4o");
        request.setDefaultModel(true);

        ModelConfig existingDefault = new ModelConfig();
        existingDefault.setId(2L);
        existingDefault.setProviderId(1L);
        existingDefault.setDefaultModel(true);

        doReturn(List.of(existingDefault)).when(modelConfigService).list(any(Wrapper.class));
        doReturn(true).when(modelConfigService).updateBatchById(anyList());
        doAnswer(inv -> {
            inv.getArgument(0, ModelConfig.class).setId(3L);
            return true;
        }).when(modelConfigService).save(any(ModelConfig.class));

        Long id = modelConfigService.createModel(request);

        assertThat(id).isEqualTo(3L);
        verify(modelConfigService).list(any(Wrapper.class));
        verify(modelConfigService).updateBatchById(anyList());
    }

    @Test
    void shouldUpdateModel_withoutClearingDefault_whenAlreadyDefault() {
        ModelConfig existing = new ModelConfig();
        existing.setId(1L);
        existing.setProviderId(1L);
        existing.setName("gpt-4o");
        existing.setDefaultModel(true);

        ModelConfigUpdateRequest request = new ModelConfigUpdateRequest();
        request.setDefaultModel(true);

        doReturn(existing).when(modelConfigService).getById(1L);
        doReturn(true).when(modelConfigService).updateById(any(ModelConfig.class));

        modelConfigService.updateModel(1L, request);

        verify(modelConfigService, never()).list(any(Wrapper.class));
        verify(modelConfigService, never()).updateBatchById(anyList());
    }

    @Test
    void shouldUpdateModel_andClearDefault_whenChangingToDefault() {
        ModelConfig existing = new ModelConfig();
        existing.setId(1L);
        existing.setProviderId(1L);
        existing.setName("gpt-4o");
        existing.setDefaultModel(false);

        ModelConfigUpdateRequest request = new ModelConfigUpdateRequest();
        request.setDefaultModel(true);

        ModelConfig otherDefault = new ModelConfig();
        otherDefault.setId(2L);
        otherDefault.setProviderId(1L);
        otherDefault.setDefaultModel(true);

        doReturn(existing).when(modelConfigService).getById(1L);
        doReturn(List.of(otherDefault)).when(modelConfigService).list(any(Wrapper.class));
        doReturn(true).when(modelConfigService).updateBatchById(anyList());
        doReturn(true).when(modelConfigService).updateById(any(ModelConfig.class));

        modelConfigService.updateModel(1L, request);

        verify(modelConfigService).list(any(Wrapper.class));
        verify(modelConfigService).updateBatchById(anyList());
    }

    @Test
    void shouldThrowBizException_whenUpdateModel_givenNonExistingId() {
        doReturn(null).when(modelConfigService).getById(999L);

        ModelConfigUpdateRequest request = new ModelConfigUpdateRequest();
        assertThatThrownBy(() -> modelConfigService.updateModel(999L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldDeleteModel_whenGivenExistingId() {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        doReturn(model).when(modelConfigService).getById(1L);
        doReturn(true).when(modelConfigService).removeById(1L);

        modelConfigService.deleteModel(1L);

        verify(modelConfigService).removeById(1L);
    }

    @Test
    void shouldThrowBizException_whenDeleteModel_givenNonExistingId() {
        doReturn(null).when(modelConfigService).getById(999L);

        assertThatThrownBy(() -> modelConfigService.deleteModel(999L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    // ========== ModelConfigApi 测试 ==========

    @Test
    void shouldReturnDto_whenGetModelByModelId_givenEnabledModel() {
        ModelConfig model = new ModelConfig();
        model.setId(1L);
        model.setModelId("gpt-4o");
        model.setName("GPT-4o");
        model.setEnabled(true);

        doReturn(model).when(modelConfigService).getOne(any());

        ModelConfigDTO dto = modelConfigService.getModelByModelId("gpt-4o");

        assertThat(dto).isNotNull();
        assertThat(dto.getModelId()).isEqualTo("gpt-4o");
        assertThat(dto.getName()).isEqualTo("GPT-4o");
    }

    @Test
    void shouldReturnNull_whenGetModelByModelId_givenNoMatch() {
        doReturn(null).when(modelConfigService).getOne(any());

        ModelConfigDTO dto = modelConfigService.getModelByModelId("non-existent");

        assertThat(dto).isNull();
    }

    @Test
    void shouldReturnDtoList_whenListEnabledModelsByProviderId() {
        ModelConfig m1 = new ModelConfig();
        m1.setId(1L);
        m1.setProviderId(1L);
        m1.setModelId("gpt-4o");
        m1.setName("GPT-4o");
        m1.setEnabled(true);
        m1.setSortOrder(1);

        ModelConfig m2 = new ModelConfig();
        m2.setId(2L);
        m2.setProviderId(1L);
        m2.setModelId("gpt-4o-mini");
        m2.setName("GPT-4o Mini");
        m2.setEnabled(true);
        m2.setSortOrder(2);

        doReturn(List.of(m1, m2)).when(modelConfigService).list(any(Wrapper.class));

        List<ModelConfigDTO> list = modelConfigService.listEnabledModelsByProviderId(1L);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getModelId()).isEqualTo("gpt-4o");
        assertThat(list.get(1).getModelId()).isEqualTo("gpt-4o-mini");
    }
}
