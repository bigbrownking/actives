package org.info.infobaza.model.dossier;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "roles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ERole name;
    @ElementCollection
    @Transient
    private List<String> relations;
    @JsonProperty(value = "person_properties")
    @Column(name = "person_properties")
    @ElementCollection
    @Transient
    private List<String> personProperties;
    @JsonProperty(value = "company_properties")
    @Column(name = "company_properties")
    @ElementCollection
    @Transient
    private List<String> companyProperties;
}
