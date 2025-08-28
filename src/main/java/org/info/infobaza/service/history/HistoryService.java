package org.info.infobaza.service.history;

import org.info.infobaza.dto.request.HistoryRequest;
import org.info.infobaza.model.main.Request;
import org.springframework.data.domain.Page;

public interface HistoryService {
    Page<Request> getRecentRequests(HistoryRequest historyRequest, int page, int size);
    Page<Request> allUsersRequests(int page, int size);
    Request createRequest(Request request);

}
