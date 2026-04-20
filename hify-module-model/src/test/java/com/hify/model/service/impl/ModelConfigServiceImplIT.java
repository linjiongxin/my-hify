package com.hify.model.service.impl;

import com.hify.model.ModelTestApplication;
import com.hify.model.api.ModelConfigApi;
import com.hify.model.api.dto.ModelConfigDTO;
import com.hify.model.dto.ModelConfigCreateRequest;
import com.hify.model.entity.ModelConfig;
import com.hify.model.service.ModelConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ModelTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql(scripts = "/sql/model-test-data.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class ModelConfigServiceImplIT {

    @Autowired
    private ModelConfigService modelConfigService;

    @Autowired
    private ModelConfigApi modelConfigApi;

    @Test
    void shouldCreateModel_andClearDefault_whenDefaultModelIsTrue() {
        // 现有数据：provider_id=1 下已有 default_model=true 的 model (id=10)
        ModelConfigCreateRequest request = new ModelConfigCreateRequest();
        request.setProviderId(1L);
        request.setName("GPT-5");
        request.setModelId("gpt-5");
        request.setDefaultModel(true);
        request.setEnabled(true);

        Long newId = modelConfigService.createModel(request);

        assertThat(newId).isNotNull();
        // 验证原来的 default model 被取消
        ModelConfig oldDefault = modelConfigService.getById(10L);
        assertThat(oldDefault.getDefaultModel()).isFalse();
        // 验证新 model 是 default
        ModelConfig newModel = modelConfigService.getById(newId);
        assertThat(newModel.getDefaultModel()).isTrue();
    }

    @Test
    void shouldNotClearDefault_whenCreateModel_givenDefaultModelIsFalse() {
        ModelConfigCreateRequest request = new ModelConfigCreateRequest();
        request.setProviderId(1L);
        request.setName("GPT-5");
        request.setModelId("gpt-5");
        request.setDefaultModel(false);
        request.setEnabled(true);

        Long newId = modelConfigService.createModel(request);

        assertThat(newId).isNotNull();
        // 原来的 default model 不受影响
        ModelConfig oldDefault = modelConfigService.getById(10L);
        assertThat(oldDefault.getDefaultModel()).isTrue();
        // 新 model 不是 default
        ModelConfig newModel = modelConfigService.getById(newId);
        assertThat(newModel.getDefaultModel()).isFalse();
    }

    @Test
    void shouldReturnModel_whenGetModelByModelId_givenEnabledModel() {
        ModelConfigDTO dto = modelConfigApi.getModelByModelId("gpt-4o");

        assertThat(dto).isNotNull();
        assertThat(dto.getName()).isEqualTo("GPT-4o");
        assertThat(dto.getModelId()).isEqualTo("gpt-4o");
    }

    @Test
    void shouldReturnNull_whenGetModelByModelId_givenDisabledModel() {
        // 先把 gpt-4o 禁用
        ModelConfig model = modelConfigService.getById(10L);
        model.setEnabled(false);
        modelConfigService.updateById(model);

        ModelConfigDTO dto = modelConfigApi.getModelByModelId("gpt-4o");

        assertThat(dto).isNull();
    }

    @Test
    void shouldReturnEnabledModels_whenListEnabledModelsByProviderId() {
        List<ModelConfigDTO> list = modelConfigApi.listEnabledModelsByProviderId(1L);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getModelId()).isEqualTo("gpt-4o");
        assertThat(list.get(1).getModelId()).isEqualTo("gpt-4o-mini");
    }
}
