package org.info.infobaza.dto.response.relation;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RelationActiveWithTypes {
    private Map<String, List<RelationActive>> typeToRelation;
}
