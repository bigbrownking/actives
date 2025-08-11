package org.info.infobaza.model.dossier;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    @Size(max = 50)
    private String iin;

    @Size(min = 8, message = "Пароль должен содержать не менее 8 символов")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[%$@&*#^!]).{8,}$",
            message = "Пароль должен содержать хотя бы одну заглавную букву, одну строчную букву, одну цифру и один специальный символ")
    @NotBlank(message = "Пароль не может быть пустым")
    private String password;

    private String email;

    private boolean active;
    private String faceimage;
    private String leftfaceimage;
    private String rightfaceimage;
    @Column(name = "faceid")
    private boolean faceId;

    private String notes;
    private String position;
    @Column(name = "department_id")
    private Long departmentId;
    @Column(name = "etanu_id")
    private Long etanuId;
    @JsonProperty(value = "ip_address")
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "token_version")
    private Integer tokenVersion = 0;

    @Column(name = "regionOfAccess")
    private String regionOfAccess;
    @Column(name = "login_time")
    private LocalDateTime loginTime;
    @Column(name = "failed_attempts")
    private Integer failedAttempts;
    @Column(name = "last_password_change_date")
    private LocalDateTime lastPasswordChangeDate;
    @Column(name = "block_reason")
    private String blockReason;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    //  @ManyToMany
//  @JoinTable(
//          name = "user_car",
//          joinColumns = @JoinColumn(name = "user_id"),
//          inverseJoinColumns = @JoinColumn(name = "car_upload_summary_id")
//  )
//  private Set<CarUploadSummary> carUploadSummaries = new HashSet<>();
    public User() {
    }

    public User(String username, String iin) {
        this.username = username;
        this.iin = iin;
    }

}
