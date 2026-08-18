package com.wegongdu.rillway.example;

import com.wegongdu.rillway.agent.spi.AgentRegistry;
import com.wegongdu.rillway.example.agent.PurchaseReviewAgent;
import com.wegongdu.rillway.policy.model.PolicyDocument;
import com.wegongdu.rillway.policy.provider.InMemoryPolicyProvider;
import com.wegongdu.rillway.policy.spi.PolicyProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PurchaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(PurchaseApplication.class, args);
    }

    @Bean
    public CommandLineRunner initPurchaseDemo(AgentRegistry agentRegistry, PolicyProvider policyProvider) {
        return args -> {
            // Register Agent
            agentRegistry.register(new PurchaseReviewAgent());

            // Register Policy
            if (policyProvider instanceof InMemoryPolicyProvider inMemoryProvider) {
                inMemoryProvider.registerDocument(PolicyDocument.of(
                        "PURCHASE_POLICY_2026",
                        "企业采购与报销合规制度(2026版)",
                        "1. 单笔小于5000元需直属领导审批；\n" +
                                "2. 5000-50000元IT与办公用品可由智能审核系统合规代批；\n" +
                                "3. 单笔超过50000元必须报批总经理；\n" +
                                "4. 禁止采购奢侈品与游戏相关设备；\n" +
                                "5. 所有报销与采购必须提供正规发票或供应商盖章报价单。",
                        "PURCHASE_POLICY_2026", "PROCUREMENT"
                ));
            }
        };
    }
}
