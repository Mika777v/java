package com.example.onboarding.controller;

import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.AssignmentStatus;
import com.example.onboarding.entity.Task;
import com.example.onboarding.entity.TaskAssignment;
import com.example.onboarding.entity.TaskChecklistItem;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.AssignmentCheckRepository;
import com.example.onboarding.repository.TaskAssignmentRepository;
import com.example.onboarding.repository.TaskChecklistItemRepository;
import com.example.onboarding.repository.TaskRepository;
import com.example.onboarding.repository.UserRepository;
import com.example.onboarding.security.AppUserDetails;
import com.example.onboarding.service.ChecklistService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.example.onboarding.SampleData.FIRST_TASK;
import static com.example.onboarding.SampleData.SATO;
import static com.example.onboarding.SampleData.SUZUKI;
import static com.example.onboarding.SampleData.TASKS_OF_SUZUKI;
import static com.example.onboarding.WebTestSupport.post;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sub-items (checklist) of tasks: managed by administrators on the task edit page,
 * checked by new employees on their own page, shown to administrators on the user detail page.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
@WithMockUser(roles = "ADMIN")
class ChecklistTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskAssignmentRepository assignmentRepository;

    @Autowired
    private TaskChecklistItemRepository itemRepository;

    @Autowired
    private AssignmentCheckRepository checkRepository;

    @Autowired
    private ChecklistService checklistService;

    @Autowired
    private EntityManager entityManager;

    /** 鈴木's second task (未着手 in the sample data) gets two sub-items. */
    private final long suzukiTask = TASKS_OF_SUZUKI.get(1);
    private TaskChecklistItem teams;
    private TaskChecklistItem sso;

    @BeforeEach
    void addItemsToSuzukisTask() {
        Task task = taskRepository.findById(suzukiTask).orElseThrow();
        teams = checklistService.addItem(task, "Teams をインストール");
        sso = checklistService.addItem(task, "SSO ログインアプリをインストール");
    }

    private User suzuki() {
        return userRepository.findById(SUZUKI).orElseThrow();
    }

    private TaskAssignment assignmentOf(long userId, long taskId) {
        entityManager.flush();
        entityManager.clear();
        return assignmentRepository.findByUserIdOrderByTaskIdAsc(userId).stream()
                .filter(a -> a.getTask().getId() == taskId)
                .findFirst().orElseThrow();
    }

    private String checkUrl(TaskAssignment assignment, TaskChecklistItem item) {
        return "/me/assignments/" + assignment.getId() + "/checklist/" + item.getId();
    }

    // --- administrators: task edit page ---

    @Test
    void editPageListsSubItems() throws Exception {
        mockMvc.perform(get("/tasks/{id}/edit", suzukiTask))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("サブ項目(チェックボックス)")))
                .andExpect(content().string(containsString("Teams をインストール")))
                .andExpect(content().string(containsString("SSO ログインアプリをインストール")));
    }

    @Test
    void editPageWithoutSubItemsSaysSo() throws Exception {
        mockMvc.perform(get("/tasks/{id}/edit", FIRST_TASK))
                .andExpect(content().string(containsString("サブ項目はありません。")));
    }

    @Test
    void newTaskPageExplainsSubItemsComeAfterRegistering() throws Exception {
        mockMvc.perform(get("/tasks/new"))
                .andExpect(content().string(containsString("登録した後の編集画面で追加できます")));
    }

    @Test
    void addingSubItemPutsItAtTheEnd() throws Exception {
        mockMvc.perform(post("/tasks/{id}/checklist", suzukiTask).param("label", "  Outlook をインストール  "))
                .andExpect(redirectedUrl("/tasks/" + suzukiTask + "/edit"))
                .andExpect(flash().attribute("message", "サブ項目「Outlook をインストール」を追加しました。"));

        assertThat(itemRepository.findByTaskIdOrderBySortOrderAscIdAsc(suzukiTask))
                .extracting(TaskChecklistItem::getLabel)
                .containsExactly("Teams をインストール", "SSO ログインアプリをインストール", "Outlook をインストール");
    }

    @Test
    void addingBlankSubItemShowsError() throws Exception {
        long before = itemRepository.count();
        mockMvc.perform(post("/tasks/{id}/checklist", suzukiTask).param("label", "  "))
                .andExpect(redirectedUrl("/tasks/" + suzukiTask + "/edit"))
                .andExpect(flash().attribute("error", "サブ項目の名前を入力してください"));
        assertThat(itemRepository.count()).isEqualTo(before);
    }

    @Test
    void addingTooLongSubItemShowsError() throws Exception {
        long before = itemRepository.count();
        mockMvc.perform(post("/tasks/{id}/checklist", suzukiTask).param("label", "a".repeat(201)))
                .andExpect(flash().attribute("error", "サブ項目の名前は200文字以内で入力してください"));
        assertThat(itemRepository.count()).isEqualTo(before);
    }

    @Test
    void addingSubItemToMissingTaskReturns404() throws Exception {
        mockMvc.perform(post("/tasks/{id}/checklist", 9999).param("label", "Teams"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingSubItemAlsoDeletesItsChecks() throws Exception {
        TaskAssignment assignment = assignmentOf(SUZUKI, suzukiTask);
        checklistService.setChecked(assignment, teams.getId(), true);

        mockMvc.perform(post("/tasks/{id}/checklist/{itemId}/delete", suzukiTask, teams.getId()))
                .andExpect(redirectedUrl("/tasks/" + suzukiTask + "/edit"))
                .andExpect(flash().attribute("message", "サブ項目「Teams をインストール」を削除しました。"));

        entityManager.flush();
        entityManager.clear();
        assertThat(itemRepository.findById(teams.getId())).isEmpty();
        assertThat(checkRepository.countByAssignmentId(assignment.getId())).isZero();
    }

    @Test
    void deletingSubItemOfAnotherTaskReturns404() throws Exception {
        mockMvc.perform(post("/tasks/{id}/checklist/{itemId}/delete", FIRST_TASK, teams.getId()))
                .andExpect(status().isNotFound());
        assertThat(itemRepository.findById(teams.getId())).isPresent();
    }

    @Test
    void deletingTaskAlsoDeletesItsSubItems() throws Exception {
        mockMvc.perform(post("/tasks/{id}/delete", suzukiTask));

        entityManager.flush();
        entityManager.clear();
        assertThat(itemRepository.findByTaskIdOrderBySortOrderAscIdAsc(suzukiTask)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "MEMBER")
    void newEmployeesCannotManageSubItems() throws Exception {
        mockMvc.perform(post("/tasks/{id}/checklist", suzukiTask).param("label", "Teams"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/tasks/{id}/checklist/{itemId}/delete", suzukiTask, teams.getId()))
                .andExpect(status().isForbidden());
    }

    // --- new employees: my page ---

    @Test
    void myPageShowsSubItemsOfTasksAlreadyAssigned() throws Exception {
        // The items were added after the task was assigned to 鈴木: they appear for him too
        mockMvc.perform(get("/me").with(user(new AppUserDetails(suzuki()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("サブ項目 0 / 2")))
                .andExpect(content().string(containsString("Teams をインストール")));
    }

    @Test
    void checkingOneItemStartsTheTask() throws Exception {
        TaskAssignment assignment = assignmentOf(SUZUKI, suzukiTask);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.NOT_STARTED);

        mockMvc.perform(post(checkUrl(assignment, teams)).param("checked", "true")
                        .with(user(new AppUserDetails(suzuki()))))
                .andExpect(redirectedUrl("/me"))
                .andExpect(flash().attribute("message", "「Teams をインストール」にチェックしました。"));

        assertThat(assignmentOf(SUZUKI, suzukiTask).getStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS);
        mockMvc.perform(get("/me").with(user(new AppUserDetails(suzuki()))))
                .andExpect(content().string(containsString("サブ項目 1 / 2")));
    }

    @Test
    void checkingAllItemsCompletesTheTask() throws Exception {
        TaskAssignment assignment = assignmentOf(SUZUKI, suzukiTask);
        mockMvc.perform(post(checkUrl(assignment, teams)).param("checked", "true")
                .with(user(new AppUserDetails(suzuki()))));

        mockMvc.perform(post(checkUrl(assignment, sso)).param("checked", "true")
                        .with(user(new AppUserDetails(suzuki()))))
                .andExpect(flash().attribute("message", containsString("を完了にしました。")));

        TaskAssignment done = assignmentOf(SUZUKI, suzukiTask);
        assertThat(done.getStatus()).isEqualTo(AssignmentStatus.DONE);
        assertThat(done.getCompletedAt()).isNotNull();
    }

    @Test
    void uncheckingACompletedTaskMovesItBackToInProgress() throws Exception {
        TaskAssignment assignment = assignmentOf(SUZUKI, suzukiTask);
        checklistService.setChecked(assignment, teams.getId(), true);
        checklistService.setChecked(assignmentOf(SUZUKI, suzukiTask), sso.getId(), true);

        mockMvc.perform(post(checkUrl(assignment, sso)).param("checked", "false")
                        .with(user(new AppUserDetails(suzuki()))))
                .andExpect(flash().attribute("message", "「SSO ログインアプリをインストール」のチェックを外しました。"));

        TaskAssignment reopened = assignmentOf(SUZUKI, suzukiTask);
        assertThat(reopened.getStatus()).isEqualTo(AssignmentStatus.IN_PROGRESS);
        assertThat(reopened.getCompletedAt()).isNull();
    }

    @Test
    void checkingTheSameItemTwiceKeepsOneCheck() throws Exception {
        TaskAssignment assignment = assignmentOf(SUZUKI, suzukiTask);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post(checkUrl(assignment, teams)).param("checked", "true")
                            .with(user(new AppUserDetails(suzuki()))))
                    .andExpect(redirectedUrl("/me"));
        }
        assertThat(checkRepository.countByAssignmentId(assignment.getId())).isEqualTo(1);
    }

    @Test
    void checkingAnotherUsersAssignmentReturns404() throws Exception {
        TaskAssignment satos = assignmentOf(SATO, suzukiTask);
        mockMvc.perform(post(checkUrl(satos, teams)).param("checked", "true")
                        .with(user(new AppUserDetails(suzuki()))))
                .andExpect(status().isNotFound());
        assertThat(checkRepository.countByAssignmentId(satos.getId())).isZero();
    }

    @Test
    void checkingItemOfAnotherTaskReturns404() throws Exception {
        TaskAssignment otherTask = assignmentOf(SUZUKI, TASKS_OF_SUZUKI.get(0));
        mockMvc.perform(post(checkUrl(otherTask, teams)).param("checked", "true")
                        .with(user(new AppUserDetails(suzuki()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void checkingMissingItemReturns404() throws Exception {
        TaskAssignment assignment = assignmentOf(SUZUKI, suzukiTask);
        mockMvc.perform(post("/me/assignments/" + assignment.getId() + "/checklist/9999").param("checked", "true")
                        .with(user(new AppUserDetails(suzuki()))))
                .andExpect(status().isNotFound());
    }

    // --- administrators: user detail ---

    @Test
    void userDetailShowsChecksWithoutButtons() throws Exception {
        checklistService.setChecked(assignmentOf(SUZUKI, suzukiTask), teams.getId(), true);

        mockMvc.perform(get("/users/{id}", SUZUKI))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("サブ項目 1 / 2")))
                .andExpect(content().string(containsString("☑")))
                .andExpect(content().string(not(containsString("/checklist/"))));
    }

    @Test
    void subItemsAppearForEveryoneTheTaskIsAssignedTo() throws Exception {
        // 佐藤 has the same task (already 完了): the items added later are shown, unchecked
        mockMvc.perform(get("/users/{id}", SATO))
                .andExpect(content().string(containsString("サブ項目 0 / 2")));
    }
}
