package org.info.infobaza.model.ser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "v_user_sso_access", schema = "oauth")
public class AccessSer {
    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "actives_search_access")
    private boolean hasAccess;
}
