package org.info.infobaza.model.main;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(name = "roles")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;

    @Type(ListArrayType.class)
    @Column(columnDefinition = "text[]")
    private List<String> relations;

    @Type(ListArrayType.class)
    @Column(columnDefinition = "text[]")
    private List<String> person_properties;

    @Type(ListArrayType.class)
    @Column(columnDefinition = "text[]")
    private List<String> company_properties;
}