package com.example.onboarding.form;

import com.example.onboarding.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Input from the user registration / edit screen.
 * Only the fields a user may change are bound, so id and timestamps cannot be set from the form.
 */
public class UserForm {

    @NotBlank(message = "氏名を入力してください")
    @Size(max = 100, message = "氏名は100文字以内で入力してください")
    private String name;

    @NotBlank(message = "メールアドレスを入力してください")
    @Email(message = "メールアドレスの形式が正しくありません")
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    private String email;

    @Size(max = 100, message = "部署は100文字以内で入力してください")
    private String department;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate joinedOn;

    public static UserForm from(User user) {
        UserForm form = new UserForm();
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        form.setDepartment(user.getDepartment());
        form.setJoinedOn(user.getJoinedOn());
        return form;
    }

    public void applyTo(User user) {
        user.setName(name.strip());
        user.setEmail(normalizedEmail());
        user.setDepartment(department == null || department.isBlank() ? null : department.strip());
        user.setJoinedOn(joinedOn);
    }

    /** Email as saved; used for the duplicate check. */
    public String normalizedEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    /** Surrounding spaces (e.g. from copy and paste) are removed before validation. */
    public void setEmail(String email) {
        this.email = email == null ? null : email.strip();
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }

    public void setJoinedOn(LocalDate joinedOn) {
        this.joinedOn = joinedOn;
    }
}
