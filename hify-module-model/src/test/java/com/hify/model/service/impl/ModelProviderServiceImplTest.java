package com.hify.model.service.impl;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.api.dto.ModelProviderDTO;
import com.hify.model.dto.ModelProviderCreateRequest;
import com.hify.model.dto.ModelProviderUpdateRequest;
import com.hify.model.entity.ModelProvider;
import com.hify.model.entity.ModelProviderStatus;
import com.hify.model.mapper.ModelProviderMapper;
import com.hify.model.mapper.ModelProviderStatusMapper;
import com.hify.model.service.ModelProviderStatusService;
import com.hify.model.vo.ModelProviderVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelProviderServiceImplTest {

    private ModelProviderServiceImpl modelProviderService;

    @Mock
    private ModelProviderMapper modelProviderMapper;

    @Mock
    private ModelProviderStatusMapper modelProviderStatusMapper;

    private ModelProviderStatusService modelProviderStatusService;

    @BeforeEach
    void setUp() {
        modelProviderStatusService = spy(new ModelProviderStatusServiceImpl());
        ReflectionTestUtils.setField(modelProviderStatusService, "baseMapper", modelProviderStatusMapper);

        modelProviderService = spy(new ModelProviderServiceImpl(modelProviderStatusService));
        ReflectionTestUtils.setField(modelProviderService, "baseMapper", modelProviderMapper);
    }

    @Test
    void shouldReturnVo_whenGetProviderDetail_givenExistingId() {
        ModelProviderVO vo = new ModelProviderVO();
        vo.setId(1L);
        vo.setName("OpenAI");
        vo.setCode("openai");
        when(modelProviderMapper.selectProviderDetail(1L)).thenReturn(vo);

        ModelProviderVO result = modelProviderService.getProviderDetail(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("OpenAI");
    }

    @Test
    void shouldThrowBizException_whenGetProviderDetail_givenNonExistingId() {
        when(modelProviderMapper.selectProviderDetail(999L)).thenReturn(null);

        assertThatThrownBy(() -> modelProviderService.getProviderDetail(999L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldCreateProvider_andInitializeStatus_whenGivenValidRequest() {
        ModelProviderCreateRequest request = new ModelProviderCreateRequest();
        request.setName("DeepSeek");
        request.setCode("deepseek");

        doAnswer(inv -> {
            inv.getArgument(0, ModelProvider.class).setId(3L);
            return true;
        }).when(modelProviderService).save(any(ModelProvider.class));
        doReturn(true).when(modelProviderStatusService).save(any(ModelProviderStatus.class));

        Long id = modelProviderService.createProvider(request);

        assertThat(id).isEqualTo(3L);
        verify(modelProviderStatusService).save(argThat(status ->
                status.getHealthStatus().equals("unknown")
        ));
    }

    @Test
    void shouldUpdateProvider_whenGivenExistingId() {
        ModelProvider existing = new ModelProvider();
        existing.setId(1L);
        existing.setName("OpenAI");

        ModelProviderUpdateRequest request = new ModelProviderUpdateRequest();
        request.setName("OpenAI Official");

        doReturn(existing).when(modelProviderService).getById(1L);
        doReturn(true).when(modelProviderService).updateById(any(ModelProvider.class));

        modelProviderService.updateProvider(1L, request);

        verify(modelProviderService).updateById(argThat(p ->
                p.getName().equals("OpenAI Official")
        ));
    }

    @Test
    void shouldThrowBizException_whenUpdateProvider_givenNonExistingId() {
        doReturn(null).when(modelProviderService).getById(999L);

        ModelProviderUpdateRequest request = new ModelProviderUpdateRequest();
        assertThatThrownBy(() -> modelProviderService.updateProvider(999L, request))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldDeleteProvider_andCascadeDeleteStatus_whenGivenExistingId() {
        ModelProvider provider = new ModelProvider();
        provider.setId(1L);
        doReturn(provider).when(modelProviderService).getById(1L);
        doReturn(true).when(modelProviderService).removeById(1L);
        doReturn(true).when(modelProviderStatusService).removeById(1L);

        modelProviderService.deleteProvider(1L);

        verify(modelProviderService).removeById(1L);
        verify(modelProviderStatusService).removeById(1L);
    }

    @Test
    void shouldThrowBizException_whenDeleteProvider_givenNonExistingId() {
        doReturn(null).when(modelProviderService).getById(999L);

        assertThatThrownBy(() -> modelProviderService.deleteProvider(999L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldReturnDtoList_withHealthStatus_whenListEnabledProviders() {
        ModelProvider p1 = new ModelProvider();
        p1.setId(1L);
        p1.setCode("openai");
        p1.setEnabled(true);

        ModelProviderStatus s1 = new ModelProviderStatus();
        s1.setProviderId(1L);
        s1.setHealthStatus("healthy");

        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ModelProvider> providerWrapper = mock(LambdaQueryChainWrapper.class);
        doReturn(providerWrapper).when(modelProviderService).lambdaQuery();
        doReturn(providerWrapper).when(providerWrapper).eq(any(), any());
        doReturn(List.of(p1)).when(providerWrapper).list();

        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ModelProviderStatus> statusWrapper = mock(LambdaQueryChainWrapper.class);
        doReturn(statusWrapper).when(modelProviderStatusService).lambdaQuery();
        doReturn(statusWrapper).when(statusWrapper).in(any(), anyCollection());
        doReturn(List.of(s1)).when(statusWrapper).list();

        List<ModelProviderDTO> result = modelProviderService.listEnabledProviders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("openai");
        assertThat(result.get(0).getHealthStatus()).isEqualTo("healthy");
    }

    @Test
    void shouldReturnEmptyList_whenListEnabledProviders_givenNoProviders() {
        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ModelProvider> providerWrapper = mock(LambdaQueryChainWrapper.class);
        doReturn(providerWrapper).when(modelProviderService).lambdaQuery();
        doReturn(providerWrapper).when(providerWrapper).eq(any(), any());
        doReturn(List.of()).when(providerWrapper).list();

        List<ModelProviderDTO> result = modelProviderService.listEnabledProviders();

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnUnknownHealthStatus_whenListEnabledProviders_givenNoStatusRecord() {
        ModelProvider p1 = new ModelProvider();
        p1.setId(1L);
        p1.setCode("openai");
        p1.setEnabled(true);

        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ModelProvider> providerWrapper = mock(LambdaQueryChainWrapper.class);
        doReturn(providerWrapper).when(modelProviderService).lambdaQuery();
        doReturn(providerWrapper).when(providerWrapper).eq(any(), any());
        doReturn(List.of(p1)).when(providerWrapper).list();

        @SuppressWarnings("unchecked")
        LambdaQueryChainWrapper<ModelProviderStatus> statusWrapper = mock(LambdaQueryChainWrapper.class);
        doReturn(statusWrapper).when(modelProviderStatusService).lambdaQuery();
        doReturn(statusWrapper).when(statusWrapper).in(any(), anyCollection());
        doReturn(List.of()).when(statusWrapper).list();

        List<ModelProviderDTO> result = modelProviderService.listEnabledProviders();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHealthStatus()).isEqualTo("unknown");
    }

    @Test
    void shouldReturnDto_whenGetProviderById_givenExistingId() {
        ModelProviderVO vo = new ModelProviderVO();
        vo.setId(1L);
        vo.setName("OpenAI");
        vo.setCode("openai");
        when(modelProviderMapper.selectProviderDetail(1L)).thenReturn(vo);

        ModelProviderDTO dto = modelProviderService.getProviderById(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("OpenAI");
    }

    @Test
    void shouldReturnNull_whenGetProviderById_givenNonExistingId() {
        when(modelProviderMapper.selectProviderDetail(999L)).thenReturn(null);

        ModelProviderDTO dto = modelProviderService.getProviderById(999L);

        assertThat(dto).isNull();
    }
}
