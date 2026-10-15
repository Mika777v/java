package com.example.onboarding.form;

import com.example.onboarding.entity.Task;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TaskForm (no database or Spring needed).
 */
class TaskFormTest {

    @Test
    void newFormIsActiveByDefault() {
        assertThat(new TaskForm().isActive()).isTrue();
    }

    @Test
    void fromCopiesValuesOfTask() {
        Task task = new Task();
        task.setTitle("Title");
        task.setDescription("Description");
        task.setActive(false);

        TaskForm form = TaskForm.from(task);

        assertThat(form.getTitle()).isEqualTo("Title");
        assertThat(form.getDescription()).isEqualTo("Description");
        assertThat(form.isActive()).isFalse();
    }

    @Test
    void applyToTrimsTitleAndCopiesValues() {
        TaskForm form = new TaskForm();
        form.setTitle("  Title  ");
        form.setDescription("Description");
        form.setActive(false);
        Task task = new Task();

        form.applyTo(task);

        assertThat(task.getTitle()).isEqualTo("Title");
        assertThat(task.getDescription()).isEqualTo("Description");
        assertThat(task.getActive()).isFalse();
    }

    @Test
    void applyToStoresBlankDescriptionAsNull() {
        TaskForm form = new TaskForm();
        form.setTitle("Title");
        form.setDescription("   ");
        Task task = new Task();

        form.applyTo(task);

        assertThat(task.getDescription()).isNull();
    }
}
