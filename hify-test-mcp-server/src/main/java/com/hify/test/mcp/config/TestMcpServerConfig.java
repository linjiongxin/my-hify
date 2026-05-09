package com.hify.test.mcp.config;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 测试 MCP Server 配置
 *
 * 注册 SSE 传输层和订单相关工具：
 * - query_order：查询订单
 * - query_logistics：查询物流
 * - apply_refund：申请退货
 *
 * @author hify
 */
@Slf4j
@Configuration
public class TestMcpServerConfig {

    // ========== Mock 数据 ==========

    private static final List<Map<String, Object>> ORDERS = new ArrayList<>();
    private static final List<Map<String, Object>> LOGISTICS = new ArrayList<>();
    private static final List<Map<String, Object>> REFUNDS = new ArrayList<>();

    static {
        // 订单 1：已完成
        ORDERS.add(Map.of(
                "orderNo", "ORD202405090001",
                "phone", "13800138001",
                "productName", "iPhone 15 Pro Max 256GB 钛金属",
                "amount", 9999.00,
                "status", "已完成",
                "statusCode", "completed",
                "createTime", "2024-05-01 10:23:15",
                "address", "北京市朝阳区建国路88号SOHO现代城A座1201室",
                "customerName", "张三"
        ));

        // 订单 2：待发货
        ORDERS.add(Map.of(
                "orderNo", "ORD202405090002",
                "phone", "13800138002",
                "productName", "Sony WH-1000XM5 降噪耳机 黑色",
                "amount", 2499.00,
                "status", "待发货",
                "statusCode", "pending_ship",
                "createTime", "2024-05-06 14:35:22",
                "address", "上海市浦东新区陆家嘴环路1000号恒生银行大厦25楼",
                "customerName", "李四"
        ));

        // 订单 3：运输中
        ORDERS.add(Map.of(
                "orderNo", "ORD202405090003",
                "phone", "13800138003",
                "productName", "Dyson V15 Detect Fluffy 吸尘器",
                "amount", 4990.00,
                "status", "运输中",
                "statusCode", "shipping",
                "createTime", "2024-05-03 09:12:08",
                "address", "广州市天河区珠江新城华夏路30号富力盈通大厦1805室",
                "customerName", "王五"
        ));

        // 订单 4：已取消
        ORDERS.add(Map.of(
                "orderNo", "ORD202405090004",
                "phone", "13800138004",
                "productName", "Nintendo Switch OLED 日版 白色",
                "amount", 2099.00,
                "status", "已取消",
                "statusCode", "cancelled",
                "createTime", "2024-05-02 16:48:33",
                "address", "深圳市南山区科技园科苑路15号科兴科学园B栋3楼",
                "customerName", "赵六"
        ));

        // 订单 5：待付款
        ORDERS.add(Map.of(
                "orderNo", "ORD202405090005",
                "phone", "13800138005",
                "productName", "MacBook Air M3 15英寸 午夜色 16G+512G",
                "amount", 13499.00,
                "status", "待付款",
                "statusCode", "pending_pay",
                "createTime", "2024-05-09 08:56:41",
                "address", "杭州市西湖区文三路478号华星时代广场A座702室",
                "customerName", "孙七"
        ));

        // 订单 6：已完成（有退货记录）
        ORDERS.add(Map.of(
                "orderNo", "ORD202405090006",
                "phone", "13800138006",
                "productName", "小米空气净化器 4 Pro",
                "amount", 1099.00,
                "status", "已完成",
                "statusCode", "completed",
                "createTime", "2024-04-28 11:17:59",
                "address", "成都市锦江区春熙路168号太古里东里2层2310号",
                "customerName", "周八"
        ));

        // 订单 7：运输中
        ORDERS.add(Map.of(
                "orderNo", "ORD202405090007",
                "phone", "13800138007",
                "productName", "华为 Mate 60 Pro 12GB+512GB 雅川青",
                "amount", 6999.00,
                "status", "运输中",
                "statusCode", "shipping",
                "createTime", "2024-05-05 20:03:27",
                "address", "武汉市江汉区建设大道568号新世界国贸大厦38楼",
                "customerName", "吴九"
        ));

        // 物流数据
        LOGISTICS.add(Map.of(
                "orderNo", "ORD202405090001",
                "company", "顺丰速运",
                "trackingNo", "SF1029384756",
                "status", "已签收",
                "traces", List.of(
                        Map.of("time", "2024-05-01 10:25", "desc", "订单已下单，等待商家发货"),
                        Map.of("time", "2024-05-01 18:30", "desc", "商家已发货，顺丰速运揽件成功"),
                        Map.of("time", "2024-05-02 06:15", "desc", "快件到达北京顺义集散中心"),
                        Map.of("time", "2024-05-02 14:20", "desc", "快件离开北京顺义集散中心，发往北京朝阳营业部"),
                        Map.of("time", "2024-05-03 09:00", "desc", "快件到达北京朝阳营业部，派送中"),
                        Map.of("time", "2024-05-03 15:40", "desc", "快件已签收，签收人：张三")
                )
        ));

        LOGISTICS.add(Map.of(
                "orderNo", "ORD202405090003",
                "company", "京东物流",
                "trackingNo", "JD8877665544",
                "status", "运输中",
                "traces", List.of(
                        Map.of("time", "2024-05-03 09:15", "desc", "订单已下单，等待商家发货"),
                        Map.of("time", "2024-05-03 21:00", "desc", "商家已发货，京东物流揽件成功"),
                        Map.of("time", "2024-05-04 07:30", "desc", "快件到达广州黄埔分拣中心"),
                        Map.of("time", "2024-05-04 23:10", "desc", "快件离开广州黄埔分拣中心，发往武汉中转场"),
                        Map.of("time", "2024-05-05 08:45", "desc", "快件到达武汉中转场")
                )
        ));

        LOGISTICS.add(Map.of(
                "orderNo", "ORD202405090007",
                "company", "中通快递",
                "trackingNo", "ZT6655443322",
                "status", "运输中",
                "traces", List.of(
                        Map.of("time", "2024-05-05 20:05", "desc", "订单已下单，等待商家发货"),
                        Map.of("time", "2024-05-06 10:20", "desc", "商家已发货，中通快递揽件成功"),
                        Map.of("time", "2024-05-06 22:00", "desc", "快件到达深圳南山分拨中心"),
                        Map.of("time", "2024-05-07 05:30", "desc", "快件离开深圳南山分拨中心，发往武汉中转部")
                )
        ));

        // 退货记录
        REFUNDS.add(Map.of(
                "refundNo", "REF202405010001",
                "orderNo", "ORD202405090006",
                "reason", "商品质量问题，滤芯有裂纹",
                "status", "退款成功",
                "applyTime", "2024-05-02 09:30:00",
                "amount", 1099.00,
                "productName", "小米空气净化器 4 Pro"
        ));
    }

    @Bean
    public ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpSseServlet() {
        var transport = HttpServletSseServerTransportProvider.builder()
                .messageEndpoint("/mcp/message")
                .sseEndpoint("/sse")
                .build();

        McpServer.sync(transport)
                .serverInfo("hify-test-mcp-server", "1.0.0")
                .tool(queryOrderTool(), this::handleQueryOrder)
                .tool(queryLogisticsTool(), this::handleQueryLogistics)
                .tool(applyRefundTool(), this::handleApplyRefund)
                .build();

        log.info("测试 MCP Server 已注册 3 个工具：query_order, query_logistics, apply_refund");

        return new ServletRegistrationBean<>(transport, "/sse", "/mcp/message");
    }

    // ========== query_order ==========

    private McpSchema.Tool queryOrderTool() {
        var properties = Map.<String, Object>of(
                "orderNo", Map.of("type", "string", "description", "订单号，如 ORD202405090001。与 phone 至少填一个"),
                "phone", Map.of("type", "string", "description", "下单手机号，如 13800138001。与 orderNo 至少填一个"),
                "status", Map.of("type", "string", "description", "订单状态筛选：completed(已完成), pending_ship(待发货), shipping(运输中), pending_pay(待付款), cancelled(已取消)。不填则查询全部")
        );
        var schema = new McpSchema.JsonSchema("object", properties, null, null, null, null);

        return McpSchema.Tool.builder()
                .name("query_order")
                .description("查询订单信息。支持按订单号、手机号查询，也可按状态筛选。")
                .inputSchema(schema)
                .build();
    }

    private McpSchema.CallToolResult handleQueryOrder(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> params) {
        String orderNo = (String) params.get("orderNo");
        String phone = (String) params.get("phone");
        String status = (String) params.get("status");

        List<Map<String, Object>> results = new ArrayList<>();
        for (Map<String, Object> order : ORDERS) {
            boolean match = true;
            if (orderNo != null && !orderNo.isBlank()) {
                match = match && orderNo.equals(order.get("orderNo"));
            }
            if (phone != null && !phone.isBlank()) {
                match = match && phone.equals(order.get("phone"));
            }
            if (status != null && !status.isBlank()) {
                match = match && status.equals(order.get("statusCode"));
            }
            if (match) {
                results.add(order);
            }
        }

        String text;
        if (results.isEmpty()) {
            text = "未找到匹配的订单。请检查订单号或手机号是否正确。\n支持查询的订单号示例：\n"
                    + ORDERS.stream().map(o -> "- " + o.get("orderNo") + "（" + o.get("customerName") + "，" + o.get("status") + "）")
                    .reduce((a, b) -> a + "\n" + b).orElse("");
        } else {
            StringBuilder sb = new StringBuilder("查询到 ").append(results.size()).append(" 条订单：\n");
            for (Map<String, Object> o : results) {
                sb.append("\n【").append(o.get("orderNo")).append("】\n")
                        .append("  商品：").append(o.get("productName")).append("\n")
                        .append("  金额：¥").append(o.get("amount")).append("\n")
                        .append("  状态：").append(o.get("status")).append("\n")
                        .append("  下单时间：").append(o.get("createTime")).append("\n")
                        .append("  收货人：").append(o.get("customerName")).append("\n")
                        .append("  手机号：").append(o.get("phone")).append("\n")
                        .append("  地址：").append(o.get("address")).append("\n");
            }
            text = sb.toString();
        }

        return McpSchema.CallToolResult.builder()
                .addTextContent(text)
                .isError(false)
                .build();
    }

    // ========== query_logistics ==========

    private McpSchema.Tool queryLogisticsTool() {
        var properties = Map.<String, Object>of(
                "orderNo", Map.of("type", "string", "description", "订单号，必填。如 ORD202405090001")
        );
        var schema = new McpSchema.JsonSchema("object", properties, List.of("orderNo"), null, null, null);

        return McpSchema.Tool.builder()
                .name("query_logistics")
                .description("查询订单物流信息。只能查询已发货或已完成的订单。")
                .inputSchema(schema)
                .build();
    }

    private McpSchema.CallToolResult handleQueryLogistics(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> params) {
        String orderNo = (String) params.get("orderNo");

        Map<String, Object> logistics = LOGISTICS.stream()
                .filter(l -> l.get("orderNo").equals(orderNo))
                .findFirst()
                .orElse(null);

        String text;
        if (logistics == null) {
            text = "未找到订单 " + orderNo + " 的物流信息。\n可能原因：\n"
                    + "1. 订单尚未发货\n"
                    + "2. 订单号输入错误\n\n"
                    + "已有物流记录的订单：ORD202405090001、ORD202405090003、ORD202405090007";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("【物流信息】\n")
                    .append("订单号：").append(logistics.get("orderNo")).append("\n")
                    .append("物流公司：").append(logistics.get("company")).append("\n")
                    .append("运单号：").append(logistics.get("trackingNo")).append("\n")
                    .append("当前状态：").append(logistics.get("status")).append("\n\n")
                    .append("【物流轨迹】\n");

            @SuppressWarnings("unchecked")
            List<Map<String, String>> traces = (List<Map<String, String>>) logistics.get("traces");
            for (Map<String, String> t : traces) {
                sb.append(t.get("time")).append("  ").append(t.get("desc")).append("\n");
            }
            text = sb.toString();
        }

        return McpSchema.CallToolResult.builder()
                .addTextContent(text)
                .isError(false)
                .build();
    }

    // ========== apply_refund ==========

    private McpSchema.Tool applyRefundTool() {
        var properties = Map.<String, Object>of(
                "orderNo", Map.of("type", "string", "description", "订单号，必填。如 ORD202405090001"),
                "reason", Map.of("type", "string", "description", "退货原因，必填。如：商品质量问题、七天无理由退货、发错货等")
        );
        var schema = new McpSchema.JsonSchema("object", properties, List.of("orderNo", "reason"), null, null, null);

        return McpSchema.Tool.builder()
                .name("apply_refund")
                .description("申请订单退货退款。订单状态必须是已完成或已签收。")
                .inputSchema(schema)
                .build();
    }

    private McpSchema.CallToolResult handleApplyRefund(
            io.modelcontextprotocol.server.McpSyncServerExchange exchange,
            Map<String, Object> params) {
        String orderNo = (String) params.get("orderNo");
        String reason = (String) params.get("reason");

        Map<String, Object> order = ORDERS.stream()
                .filter(o -> o.get("orderNo").equals(orderNo))
                .findFirst()
                .orElse(null);

        String text;
        if (order == null) {
            text = "退货申请失败：订单 " + orderNo + " 不存在。";
        } else if (!("completed".equals(order.get("statusCode")))) {
            text = "退货申请失败：订单 " + orderNo + " 当前状态为「" + order.get("status") + "」，只有已完成的订单才能申请退货。";
        } else {
            boolean hasRefund = REFUNDS.stream().anyMatch(r -> r.get("orderNo").equals(orderNo));
            if (hasRefund) {
                text = "退货申请失败：订单 " + orderNo + " 已有退货记录，请勿重复申请。";
            } else {
                String refundNo = "REF" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                Map<String, Object> refund = new LinkedHashMap<>();
                refund.put("refundNo", refundNo);
                refund.put("orderNo", orderNo);
                refund.put("reason", reason);
                refund.put("status", "退款审核中");
                refund.put("applyTime", LocalDateTime.now().toString());
                refund.put("amount", order.get("amount"));
                refund.put("productName", order.get("productName"));
                REFUNDS.add(refund);

                text = "退货申请提交成功！\n\n"
                        + "【申请信息】\n"
                        + "退货单号：" + refundNo + "\n"
                        + "订单号：" + orderNo + "\n"
                        + "商品：" + order.get("productName") + "\n"
                        + "退款金额：¥" + order.get("amount") + "\n"
                        + "退货原因：" + reason + "\n"
                        + "申请时间：" + refund.get("applyTime") + "\n"
                        + "当前状态：退款审核中\n\n"
                        + "审核预计需要 1-3 个工作日，请耐心等待。";
            }
        }

        return McpSchema.CallToolResult.builder()
                .addTextContent(text)
                .isError(false)
                .build();
    }
}
