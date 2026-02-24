package org.info.infobaza.dto.response.info.active;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.info.infobaza.dto.response.info.ActiveCountGroup;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class ActiveUl implements ActiveResponse {
    private String dateFrom;
    private String dateTo;
    private List<String> selectedYears;
    private List<ActiveCountGroup> aktivyTypeCounts;
    private Map<String, List<String>> iinToRelation;

    private int totalInfoRecords;
    private int totalEsfRecords;
    private long totalElements;
    private int currentPage;
    private int pageSize;
    private int totalPages;

    public boolean hasNext() {
        return currentPage < totalPages - 1;
    }

    public boolean hasPrevious() {
        return currentPage > 0;
    }

    public boolean isFirst() {
        return currentPage == 0;
    }

    public boolean isLast() {
        return currentPage >= totalPages - 1;
    }
}