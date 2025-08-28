package org.info.infobaza.util.convert;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class JpaPageable {
    public static Pageable createPageable(Integer page, Integer size) {
        return PageRequest.of(page, size);
    }
    public static Pageable createPageableSorted(Integer page, Integer size, Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}
