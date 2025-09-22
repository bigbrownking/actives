package org.info.infobaza.service.relations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.Dictionary;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.dto.response.relation.RelationActive;
import org.info.infobaza.dto.response.relation.RelationActiveWithTypes;
import org.info.infobaza.model.info.person.RelationRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.info.infobaza.constants.Dictionary.SECONDARY_STATUSES;
import static org.info.infobaza.service.Analyzer.keepDistinctRelations;

@Service
@Slf4j
@RequiredArgsConstructor
public class RelationService {

    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;
    private final Analyzer analyzer;

    public RelationActiveWithTypes getPrimaryRelationsOfPerson(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching primary relations for IIN: {}", iin);

        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Связи_Семейные.getPath(), iin);
        List<RelationRecord> relations = jdbcTemplate.query(sql, mapper::mapRowToRelation);

        List<RelationActive> relationActives = analyzer.toRelationActives(keepDistinctRelations(relations), dateFrom, dateTo);

        Map<String, List<RelationActive>> levelToRelation = relationActives.stream()
                .collect(Collectors.groupingBy(
                        ra -> "Уровень " + ra.getLevel()
                ));

        return getRelationActiveWithTypes(levelToRelation);
    }

    public RelationActiveWithTypes getSecondaryRelationsOfPerson(String iin, String dateFrom, String dateTo) throws IOException {
        log.info("Fetching secondary relations for IIN: {}", iin);
        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Связи_Косвенные.getPath(), iin);
        List<RelationRecord> relations = jdbcTemplate.query(sql, mapper::mapRowToRelation);

        Map<String, List<String>> groupedRelations = relations.stream()
                .collect(Collectors.groupingBy(
                        RelationRecord::getIin_2,
                        Collectors.mapping(RelationRecord::getStatus, Collectors.toList())
                ));

        List<RelationRecord> processedRelations = groupedRelations.entrySet().stream()
                .map(entry -> {
                    String iin2 = entry.getKey();
                    List<String> rawStatuses = entry.getValue();
                    Set<String> uniqueStatuses = new LinkedHashSet<>(rawStatuses);
                    List<String> statuses = new ArrayList<>(uniqueStatuses);
                    List<String> processedStatuses = statuses.stream().map(status -> {
                        if (status.equals("Доверенность")) {
                            Optional<RelationRecord> matchingRecord = relations.stream()
                                    .filter(r -> r.getIin_2().equals(iin2) &&
                                            r.getStatus().equals(status) && r.getIin_1().equals(iin))
                                    .findFirst();
                            if (matchingRecord.isPresent()) {
                                RelationRecord record = matchingRecord.get();
                                String vidSviazi = record.getVid_sviazi();
                                if (vidSviazi != null && !vidSviazi.isEmpty()) {
                                    String iin1 = record.getIin_1();
                                    String iin2Value = record.getIin_2();
                                    if (vidSviazi.startsWith(iin1)) {
                                        return "Доверитель";
                                    } else if (vidSviazi.startsWith(iin2Value)) {
                                        return "Поверенный";
                                    }
                                }
                            }
                        }
                        return status;
                    }).collect(Collectors.toList());

                    String concatenatedStatus = String.join(", ", processedStatuses);
                    RelationRecord representative = relations.stream()
                            .filter(r -> r.getIin_2().equals(iin2))
                            .findFirst()
                            .orElse(null);
                    if (representative != null) {
                        representative.setStatus(concatenatedStatus);
                    }
                    return representative;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<RelationActive> relationActives = analyzer.toRelationActives(processedRelations, dateFrom, dateTo);
        Map<String, List<RelationActive>> typeToRelation = new HashMap<>();
        for (RelationRecord rr : processedRelations) {
            String status = rr.getStatus();
            String firstStatus = status.split(",")[0].trim();
            String category = SECONDARY_STATUSES.get(firstStatus);
            if (category != null) {
                relationActives.stream()
                        .filter(active -> active.getIin().equals(rr.getIin_2()))
                        .findFirst().ifPresent(ra -> {
                            typeToRelation.computeIfAbsent(category, k -> new ArrayList<>()).add(ra);
                        });
            }
        }

        return getRelationActiveWithTypes(typeToRelation);
    }
    private RelationActiveWithTypes getRelationActiveWithTypes(Map<String, List<RelationActive>> typeToRelation) {
        typeToRelation.forEach((key, list) ->
                list.sort(Comparator.comparing(ra -> {
                    try {
                        String cleanedActives = ra.getActives().replaceAll("[^0-9.]", "");
                        return Double.parseDouble(cleanedActives);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid actives value for IIN {}: {}. Defaulting to 0.0", ra.getIin(), ra.getActives());
                        return 0.0;
                    }
                }, Comparator.reverseOrder()))
        );

        RelationActiveWithTypes result = new RelationActiveWithTypes();
        result.setTypeToRelation(typeToRelation);
        return result;
    }


}