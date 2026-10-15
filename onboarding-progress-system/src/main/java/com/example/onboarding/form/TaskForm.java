package com.example.onboarding.form;

import com.example.onboarding.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Input from the task registration / edit screen.
 * Only the fields a user may change are bound, so id and timestamps cannot be set from the form.
 */
public class TaskForm {

    @NotBlank(message = "タイトルを入力してください")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;

    private String description;

    private boolean active = true;

    public static TaskForm from(Task task) {
        TaskForm form = new TaskForm();
        form.setTitle(task.getTitle());
        form.setDescription(task.getDescription());
        form.setActive(Boolean.TRUE.equals(task.getActive()));
        return form;
    }

    public void applyTo(Task task) {
        task.setTitle(title.strip());
        task.setDescription(description == null || description.isBlank() ? null : description);
        task.setActive(active);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
