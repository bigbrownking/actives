package org.info.infobaza.model.main;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "actives_requests")
public class Request {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "iin_bin")
    private String iinBin;

    @Column(name = "date_from")
    private LocalDate dateFrom;

    @Column(name = "date_to")
    private LocalDate dateTo;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "search")
    private String search;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @ElementCollection
    @CollectionTable(name = "actives_request_years", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "year")
    private List<String> years;

    @ElementCollection
    @CollectionTable(name = "actives_request_vids", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "vid")
    private List<String> vids;

    @ElementCollection
    @CollectionTable(name = "actives_request_types", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "type")
    private List<String> types;

    @ElementCollection
    @CollectionTable(name = "actives_request_sources", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "source")
    private List<String> sources;

    @ElementCollection
    @CollectionTable(name = "actives_request_iins", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "iin")
    private List<String> iins;
}