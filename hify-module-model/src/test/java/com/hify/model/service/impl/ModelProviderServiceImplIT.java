package com.hify.model.service.impl;

import com.hify.common.core.enums.ResultCode;
import com.hify.common.core.exception.BizException;
import com.hify.model.ModelTestApplication;
import com.hify.model.api.dto.ModelProviderDTO;
import com.hify.model.dto.ModelProviderCreateRequest;
import com.hify.model.dto.ModelProviderUpdateRequest;
import com.hify.model.entity.ModelProviderStatus;
import com.hify.model.mapper.ModelProviderStatusMapper;
import com.hify.model.api.ModelProviderApi;
import com.hify.model.service.ModelProviderService;
import com.hify.model.vo.ModelProviderVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ModelTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/model-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ModelProviderServiceImplIT {

    @Autowired
    private ModelProviderService modelProviderService;

    @Autowired
    private ModelProviderApi modelProviderApi;

    @Autowired
    private ModelProviderStatusMapper modelProviderStatusMapper;

    @Test
    void shouldCreateProvider_andInitializeStatus_whenGivenValidRequest() {
        ModelProviderCreateRequest request = new ModelProviderCreateRequest();
        request.setName("Qwen");
        request.setCode("qwen");
        request.setProtocolType("openai_compatible");
        request.setApiBaseUrl("https://dashscope.aliyuncs.com/v1");
        request.setAuthType("BEARER");
        request.setApiKey("sk-test");
        request.setEnabled(true);

        Long id = modelProviderService.createProvider(request);

        assertThat(id).isNotNull();
        // 验证 status 记录已自动创建
        ModelProviderStatus status = modelProviderStatusMapper.selectById(id);
        assertThat(status).isNotNull();
        assertThat(status.getHealthStatus()).isEqualTo("unknown");
    }

    @Test
    void shouldDeleteProvider_andCascadeDeleteStatus_whenGivenExistingId() {
        // provider 1 有 status 记录
        modelProviderService.deleteProvider(1L);

        // status 记录应被级联物理删除
        ModelProviderStatus status = modelProviderStatusMapper.selectById(1L);
        assertThat(status).isNull();
    }

    @Test
    void shouldReturnProvidersWithHealthStatus_whenListEnabledProviders() {
        List<ModelProviderDTO> list = modelProviderApi.listEnabledProviders();

        // 只有 enabled=true 的 provider（id=1）
        assertThat(list).hasSize(1);
        ModelProviderDTO dto = list.get(0);
        assertThat(dto.getCode()).isEqualTo("openai");
        assertThat(dto.getHealthStatus()).isEqualTo("healthy");
    }

    @Test
    void shouldThrowBizException_whenGetProviderDetail_givenNonExistingId() {
        assertThatThrownBy(() -> modelProviderService.getProviderDetail(999L))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(ResultCode.DATA_NOT_FOUND.getCode());
                });
    }

    @Test
    void shouldReturnProvider_whenGetProviderDetail_givenExistingId() {
        ModelProviderVO vo = modelProviderService.getProviderDetail(1L);

        assertThat(vo).isNotNull();
        assertThat(vo.getName()).isEqualTo("OpenAI");
        assertThat(vo.getCode()).isEqualTo("openai");
        assertThat(vo.getHealthStatus()).isEqualTo("healthy");
    }

    @Test
    void shouldUpdateProvider_whenGivenValidRequest() {
        ModelProviderUpdateRequest request = new ModelProviderUpdateRequest();
        request.setName("OpenAI Official");
        request.setApiBaseUrl("https://api.openai-official.com/v1");

        modelProviderService.updateProvider(1L, request);

        ModelProviderVO vo = modelProviderService.getProviderDetail(1L);
        assertThat(vo.getName()).isEqualTo("OpenAI Official");
        assertThat(vo.getApiBaseUrl()).isEqualTo("https://api.openai-official.com/v1");
    }
}
