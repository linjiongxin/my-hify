package com.hify.model.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hify.model.ModelTestApplication;
import com.hify.model.vo.ModelProviderVO;
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
class ModelProviderMapperIT {

    @Autowired
    private ModelProviderMapper modelProviderMapper;

    @Test
    void shouldReturnVoWithHealthStatus_whenSelectProviderPage_givenJoinedData() {
        Page<ModelProviderVO> page = new Page<>(1, 10);

        Page<ModelProviderVO> result = modelProviderMapper.selectProviderPage(page, false);
        List<ModelProviderVO> list = result.getRecords();

        assertThat(list).hasSize(2);
        // provider 1 有 status 记录，health_status = 'healthy'
        ModelProviderVO openai = list.stream()
                .filter(p -> "openai".equals(p.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(openai.getHealthStatus()).isEqualTo("healthy");
        // provider 2 没有 status 记录，health_status = null
        ModelProviderVO deepseek = list.stream()
                .filter(p -> "deepseek".equals(p.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(deepseek.getHealthStatus()).isNull();
    }

    @Test
    void shouldReturnVoWithNullHealthStatus_whenSelectProviderPage_givenNoStatusRecord() {
        Page<ModelProviderVO> page = new Page<>(1, 10);

        Page<ModelProviderVO> result = modelProviderMapper.selectProviderPage(page, false);
        List<ModelProviderVO> list = result.getRecords();

        ModelProviderVO deepseek = list.stream()
                .filter(p -> "deepseek".equals(p.getCode()))
                .findFirst()
                .orElseThrow();
        assertThat(deepseek.getHealthStatus()).isNull();
    }

    @Test
    void shouldReturnVoWithHealthStatus_whenSelectProviderDetail_givenExistingId() {
        ModelProviderVO vo = modelProviderMapper.selectProviderDetail(1L);

        assertThat(vo).isNotNull();
        assertThat(vo.getName()).isEqualTo("OpenAI");
        assertThat(vo.getCode()).isEqualTo("openai");
        assertThat(vo.getHealthStatus()).isEqualTo("healthy");
    }

    @Test
    void shouldReturnNull_whenSelectProviderDetail_givenNonExistingId() {
        ModelProviderVO vo = modelProviderMapper.selectProviderDetail(999L);

        assertThat(vo).isNull();
    }
}
