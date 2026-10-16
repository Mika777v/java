package com.example.onboarding.controller;

import com.example.onboarding.SampleData;
import com.example.onboarding.TestClockConfiguration;
import com.example.onboarding.TestcontainersConfiguration;
import com.example.onboarding.entity.User;
import com.example.onboarding.repository.TaskAssignmentRepository;
import com.example.onboarding.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.example.onboarding.WebTestSupport.post;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Two requests at the same time (a double click, or two administrators) can both pass the
 * "already exists?" check before either one saves. The database constraints are the last line of
 * defense, and the screens must show the usual message instead of an error page.
 * The race is reproduced by making the check answer "not yet" although the row already exists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestClockConfiguration.class})
@Transactional
@WithMockUser(roles = "ADMIN")
class ConcurrentRequestTest {

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private UserRepository userRepository;

    @SpyBean
    private TaskAssignmentRepository assignmentRepository;

    @Test
    void databaseRejectsEmailThatDiffersOnlyInCase() {
        User user = new User();
        user.setName("山田 太郎(2人目)");
        user.setEmail("YAMADA@example.com");

        assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void registeringSameEmailAtTheSameTimeShowsDuplicateMessage() throws Exception {
        doReturn(false).when(userRepository).existsByEmailIgnoreCase(anyString());

        mockMvc.perform(post("/users")
                        .param("name", "山田 太郎(2人目)")
                        .param("email", "Yamada@example.com")
                        .param("role", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("このメールアドレスは既に登録されています")));
    }

    @Test
    void assigningSameTaskAtTheSameTimeShowsAlreadyAssignedMessage() throws Exception {
        doReturn(false).when(assignmentRepository).existsByUserIdAndTaskId(anyLong(), anyLong());

        mockMvc.perform(post("/users/{id}/assignments", SampleData.SUZUKI)
                        .param("taskId", String.valueOf(SampleData.TASKS_OF_SUZUKI.get(0))))
                .andExpect(redirectedUrl("/users/" + SampleData.SUZUKI))
                .andExpect(flash().attribute("error", "このタスクは既に割り当てられています。"));
    }

    @Test
    void assigningAllTasksAtTheSameTimeShowsAlreadyAssignedMessage() throws Exception {
        // The other request has just assigned these tasks: the list of assignable tasks is outdated
        doReturn(java.util.List.of()).when(assignmentRepository).findByUserIdOrderByTaskSortOrderAscTaskIdAsc(any());

        mockMvc.perform(post("/users/{id}/assignments/all", SampleData.SUZUKI))
                .andExpect(redirectedUrl("/users/" + SampleData.SUZUKI))
                .andExpect(flash().attribute("error", "このタスクは既に割り当てられています。"));
    }
}
