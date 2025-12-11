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
@Table(name = "mv_ul", schema = "imp_kfm_ul")
public class MvUl {
    @Nullable
    private String bin;
    @Nullable
    private String full_name_rus;
    @Nullable
    private String full_name_kaz;
    @Nullable
    private String org_status;
    @Nullable
    private String org_registration_type;
    @Nullable
    private String org_form;
    @Nullable
    private String org_reg_date;
    @Nullable
    private String legal_form;
    @Nullable
    private String is_resident;
    @Nullable
    private String registration_agensy;
    @Nullable
    private String registration_number;
    @Nullable
    private String oked;
    @Nullable
    private String head_organization;
    @Nullable
    @Id
    private String subject_id;
    @Nullable
    private String layer_id;
    @Nullable
    private String ul_status;
    @Nullable
    @Transient
    private Boolean is_upd;
    @Nullable
    private String short_name;
}