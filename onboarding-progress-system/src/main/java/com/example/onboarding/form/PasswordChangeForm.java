package com.example.onboarding.form;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * Input from the "パスワード変更" screen (/me/password).
 * The checks that need the database (is the current password right?) are done in the controller.
 */
public class PasswordChangeForm {

    @NotEmpty(message = "現在のパスワードを入力してください")
    private String currentPassword;

    @NotEmpty(message = "新しいパスワードを入力してください")
    @Size(min = UserForm.MIN_PASSWORD_LENGTH, max = 72, message = "パスワードは8文字以上72文字以内で入力してください")
    private String newPassword;

    @NotEmpty(message = "確認のため、新しいパスワードをもう一度入力してください")
    private String confirmPassword;

    /** Passwords are never shown again when the form is redisplayed. */
    public void clear() {
        currentPassword = null;
        newPassword = null;
        confirmPassword = null;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
