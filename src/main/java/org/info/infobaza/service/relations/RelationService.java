package org.info.infobaza.service.relations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.constants.Dictionary;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.dto.response.relation.RelationActive;
import org.info.infobaza.dto.response.relation.RelationActiveWithTypes;
import org.info.infobaza.model.info.person.RelationRecord;
import org.info.infobaza.service.Analyzer;
import org.info.infobaza.util.convert.IinChecker;
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
    private final IinChecker iinChecker;
    private final Analyzer analyzer;

    public RelationActiveWithTypes getPrimaryRelationsOfPerson(String iin, String dateFrom, String dateTo) throws IOException {
        if (iin == null || iin.trim().isEmpty()) {
            throw new IllegalArgumentException("IIN cannot be null or empty");
        }
        log.info("Fetching primary relations for IIN: {}", iin);

        String sql = sqlFileUtil.getSqlWithIin(QueryLocationDictionary.Связи_Семейные.getPath(), iin);
        List<RelationRecord> relations = jdbcTemplate.query(sql, mapper::mapRowToPriRelation);
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
        List<RelationRecord> relations = jdbcTemplate.query(sql, mapper::mapRowToSecRelation);

        relations = relations.stream()
                .filter(relation -> !IinChecker.isUl(relation.getIin_2()))
                .collect(Collectors.toList());

        // Step 1: Process "Доверенность" statuses individually
        for (RelationRecord relation : relations) {
            if (relation.getStatus().equals("Доверенность")) {
                String vidSviazi = relation.getVid_sviazi();
                if (vidSviazi != null && !vidSviazi.isEmpty()) {
                    String iin1 = relation.getIin_1();
                    String iin2 = relation.getIin_2();
                    if (vidSviazi.startsWith(iin1)) {
                        relation.setStatus("Доверитель");
                    } else if (vidSviazi.startsWith(iin2)) {
                        relation.setStatus("Поверенный");
                    }
                }
            }
        }

        // Step 2: Convert ALL records to RelationActive
        List<RelationActive> relationActives = analyzer.toRelationActives(relations, dateFrom, dateTo);

        Map<String, RelationActive> uniqueRelationActives = new HashMap<>();
        int relationIndex = 0;

        for (RelationRecord relation : relations) {
            String iin2 = relation.getIin_2();
            Map<String, String> dopinfo = relation.getDopinfo();
            String uniqueKey = iin2 + "|" + (dopinfo != null ? dopinfo.toString() : "null");

            if (relationIndex < relationActives.size() && !uniqueRelationActives.containsKey(uniqueKey)) {
                RelationActive ra = relationActives.get(relationIndex);
                uniqueRelationActives.put(uniqueKey, ra);
            }
            relationIndex++;
        }

        List<RelationActive> finalRelationActives = new ArrayList<>(uniqueRelationActives.values());

        // Step 4: Categorize unique RelationActive objects
        Map<String, List<RelationActive>> typeToRelation = new HashMap<>();

        for (RelationActive ra : finalRelationActives) {
            String status = ra.getRelation();
            if (status == null || status.isEmpty()) continue;

            String cleanedStatus = status.trim();
            if (cleanedStatus.isEmpty()) continue;

            String category = SECONDARY_STATUSES.getOrDefault(cleanedStatus, "Прочие");
            typeToRelation.computeIfAbsent(category, k -> new ArrayList<>()).add(ra);
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