package com.example.onboarding.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Input of the "add a sub-item" form on the task edit page.
 */
public class ChecklistItemForm {

    @NotBlank(message = "サブ項目の名前を入力してください")
    @Size(max = 200, message = "サブ項目の名前は200文字以内で入力してください")
    private String label;

    public String normalizedLabel() {
        return label.strip();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
