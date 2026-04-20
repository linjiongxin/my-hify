package com.hify.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hify.agent.entity.Agent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AgentMapper extends BaseMapper<Agent> {
}