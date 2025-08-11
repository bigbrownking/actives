package org.info.infobaza.model.ser;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;


@Entity
@Data
@Table(name = "users", schema = "oauth")
public class UserSer {

    @Id
    private Long id;

    @Column(name = "iin")
    private String iin;

    @Column(name = "email")
    private String email;

    @NotBlank
    @Size(max = 120)
    private String password;

    @Column(name = "password_expiration_date")
    private LocalDateTime passwordExpDate;

    @Column(name = "status")
    private String status;

    @Column(name = "dossier_access_category")
    private String access;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}


