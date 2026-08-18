package com.wegongdu.rillway.example;

import com.wegongdu.rillway.ai.cache.ResolutionCacheManager;
import com.wegongdu.rillway.ai.cache.ResolutionCacheRepository;
import com.wegongdu.rillway.core.context.ProcessContext;
import com.wegongdu.rillway.core.definition.ProcessDefinition;
import com.wegongdu.rillway.core.identity.IdentityService;
import com.wegongdu.rillway.core.identity.UserProfile;
import com.wegongdu.rillway.core.model.ResolutionCache;
import com.wegongdu.rillway.core.model.Task;
import com.wegongdu.rillway.core.model.TaskStatus;
import com.wegongdu.rillway.runtime.engine.ProcessEngine;
import com.wegongdu.rillway.runtime.identity.DefaultIdentityService;
import com.wegongdu.rillway.runtime.task.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PurchaseApplication.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rillway_cache_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class ResolutionCacheIntegrationTest {

    @Autowired
    private ProcessEngine processEngine;

    @Autowired
    private TaskService taskService;

    @Autowired
    private IdentityService identityService;

    @Autowired
    private ResolutionCacheRepository resolutionCacheRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ProcessDefinition expenseWorkflow;

    @BeforeEach
    void setupOrg() {
        if (identityService instanceof DefaultIdentityService defaultIdentity) {
            defaultIdentity.registerDepartmentManager("DEPT_RD", "Director_David");

            defaultIdentity.registerUserProfile(UserProfile.builder("Alice")
                    .departmentId("DEPT_RD")
                    .username("爱丽丝")
                    .build());

            defaultIdentity.registerUserProfile(UserProfile.builder("Bob")
                    .departmentId("DEPT_RD")
                    .username("鲍勃")
                    .build());
        }

        expenseWorkflow = ProcessDefinition.builder("cached-expense-workflow")
                .startNode("start")
                .humanNode("dept-head-approval", h -> h
                        .name("部门主管审批")
                        .assigneePrompt("申请人所在部门负责人审批")
                )
                .endNode("end")
                .edge("start", "dept-head-approval")
                .edge("dept-head-approval", "end")
                .build();
    }

    @Test
    @DisplayName("should record cache on first run, hit cache on second run, and invalidate upon org change")
    void should_cache_and_verify_accurately() {
        String prompt = "申请人所在部门负责人审批";
        String promptHash = ResolutionCacheManager.computePromptHash(prompt);

        // 1. First Execution by Alice (R&D department) -> LLM resolves and records cache
        String key1 = "EXPENSE_001";
        processEngine.start(expenseWorkflow, key1, ProcessContext.builder().initiator("Alice").build());

        List<Task> aliceTasks = taskService.findPendingTasks("Director_David", List.of());
        assertThat(aliceTasks).anyMatch(t -> key1.equals(t.businessKey()));

        // Verify that the resolution cache was recorded into the database
        Optional<ResolutionCache> cacheRecord = resolutionCacheRepository.findMatch(
                "dept-head-approval", "dept-head-approval", promptHash, "DEPT_RD", null
        );
        assertThat(cacheRecord).isPresent();
        assertThat(cacheRecord.get().resolvedUserId()).isEqualTo("Director_David");
        assertThat(cacheRecord.get().hitCount()).isEqualTo(0);

        // 2. Second Execution by Bob (same R&D department) -> Hits Fast-Path cache with 0 Token & hitCount incremented!
        String key2 = "EXPENSE_002";
        processEngine.start(expenseWorkflow, key2, ProcessContext.builder().initiator("Bob").build());

        List<Task> bobTasks = taskService.findPendingTasks("Director_David", List.of());
        assertThat(bobTasks).anyMatch(t -> key2.equals(t.businessKey()));

        Optional<ResolutionCache> updatedCache = resolutionCacheRepository.findMatch(
                "dept-head-approval", "dept-head-approval", promptHash, "DEPT_RD", null
        );
        assertThat(updatedCache).isPresent();
        assertThat(updatedCache.get().hitCount()).isGreaterThanOrEqualTo(1);

        // 3. Organizational Change happens: R&D Director changed to Director_NewDavid
        if (identityService instanceof DefaultIdentityService defaultIdentity) {
            defaultIdentity.registerDepartmentManager("DEPT_RD", "Director_NewDavid");
        }

        // 4. Third Execution by Alice -> Cache verification detects mismatch, invalidates stale cache, and re-resolves to new Director!
        String key3 = "EXPENSE_003";
        processEngine.start(expenseWorkflow, key3, ProcessContext.builder().initiator("Alice").build());

        List<Task> newDirectorTasks = taskService.findPendingTasks("Director_NewDavid", List.of());
        assertThat(newDirectorTasks).anyMatch(t -> key3.equals(t.businessKey()));

        // Verify updated cache points to Director_NewDavid
        Optional<ResolutionCache> finalCache = resolutionCacheRepository.findMatch(
                "dept-head-approval", "dept-head-approval", promptHash, "DEPT_RD", null
        );
        assertThat(finalCache).isPresent();
        assertThat(finalCache.get().resolvedUserId()).isEqualTo("Director_NewDavid");
    }
}
