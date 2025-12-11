package org.info.infobaza.model.dossierprime;

import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "mv_fl",
        schema = "imp_kfm_fl"
)
public class MvFl {
    @Id
    private String id;
    private @Nullable String iin;
    private @Nullable String last_name;
    private @Nullable String first_name;
    private @Nullable String patronymic;
    private @Nullable String birth_date;
    private @Nullable String citizenship_id;
    @Transient
    private @Nullable String citizenship_ru_name = "КАЗАХСТАН";
    private @Nullable String nationality_id;
    private @Nullable String nationality_ru_name;
    private @Nullable boolean is_resident;
    private @Nullable String life_status_id;
    private @Nullable String life_status_ru_name;
    private @Nullable String death_date;
    private String district;
    private String region;
}
