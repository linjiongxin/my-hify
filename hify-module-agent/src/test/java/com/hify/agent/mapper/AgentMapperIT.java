package com.hify.agent.mapper;

import com.hify.agent.AgentTestApplication;
import com.hify.agent.entity.Agent;
import com.hify.model.api.ModelConfigApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AgentTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class AgentMapperIT {

    @MockBean
    private ModelConfigApi modelConfigApi;

    @Autowired
    private AgentMapper agentMapper;

    @BeforeEach
    void setUp() {
        // Clear existing data
        agentMapper.delete(null);

        // Insert test data
        Agent agent1 = new Agent();
        agent1.setId(1L);
        agent1.setName("TestAgent1");
        agent1.setDescription("Description1");
        agent1.setModelId("gpt-4o");
        agent1.setSystemPrompt("You are helpful.");
        agent1.setTemperature(new BigDecimal("0.7"));
        agent1.setMaxTokens(2048);
        agent1.setTopP(new BigDecimal("1.0"));
        agent1.setWelcomeMessage("Welcome to TestAgent1");
        agent1.setEnabled(true);
        agent1.setCreatedAt(LocalDateTime.now());
        agent1.setUpdatedAt(LocalDateTime.now());
        agent1.setDeleted(false);
        agentMapper.insert(agent1);

        Agent agent2 = new Agent();
        agent2.setId(2L);
        agent2.setName("TestAgent2");
        agent2.setDescription("Description2");
        agent2.setModelId("gpt-4o-mini");
        agent2.setSystemPrompt("You are smart.");
        agent2.setTemperature(new BigDecimal("0.5"));
        agent2.setMaxTokens(1024);
        agent2.setTopP(new BigDecimal("0.9"));
        agent2.setWelcomeMessage("Welcome to TestAgent2");
        agent2.setEnabled(true);
        agent2.setCreatedAt(LocalDateTime.now());
        agent2.setUpdatedAt(LocalDateTime.now());
        agent2.setDeleted(false);
        agentMapper.insert(agent2);
    }

    @Test
    void shouldSelectAllEnabledAgents() {
        List<Agent> agents = agentMapper.selectList(null);
        assertThat(agents).hasSize(2);
    }

    @Test
    void shouldSelectAgentById() {
        Agent agent = agentMapper.selectById(1L);
        assertThat(agent).isNotNull();
        assertThat(agent.getName()).isEqualTo("TestAgent1");
    }

    @Test
    void shouldSelectAgentByName() {
        List<Agent> agents = agentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Agent>()
                .eq(Agent::getName, "TestAgent1")
        );
        assertThat(agents).hasSize(1);
        assertThat(agents.get(0).getModelId()).isEqualTo("gpt-4o");
    }

    @Test
    void shouldSelectEnabledAgentsOnly() {
        List<Agent> agents = agentMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Agent>()
                .eq(Agent::getEnabled, true)
        );
        assertThat(agents).hasSize(2);
    }

    @Test
    void shouldReturnNull_whenSelectByNonExistingId() {
        Agent agent = agentMapper.selectById(999L);
        assertThat(agent).isNull();
    }

    @Test
    void shouldVerifyAgentFields() {
        Agent agent = agentMapper.selectById(1L);
        assertThat(agent).isNotNull();
        assertThat(agent.getDescription()).isEqualTo("Description1");
        assertThat(agent.getModelId()).isEqualTo("gpt-4o");
        assertThat(agent.getSystemPrompt()).isEqualTo("You are helpful.");
        assertThat(agent.getTemperature()).isEqualByComparingTo("0.7");
        assertThat(agent.getMaxTokens()).isEqualTo(2048);
        assertThat(agent.getTopP()).isEqualByComparingTo("1.0");
        assertThat(agent.getWelcomeMessage()).isEqualTo("Welcome to TestAgent1");
        assertThat(agent.getEnabled()).isTrue();
        assertThat(agent.getDeleted()).isFalse();
    }
}