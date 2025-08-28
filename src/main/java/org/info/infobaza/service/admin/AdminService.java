package org.info.infobaza.service.admin;

import org.info.infobaza.dto.request.LogSearchRequest;
import org.info.infobaza.model.info.log.Logs;
import org.springframework.data.domain.Page;

public interface AdminService {
    Page<Logs> searchFromLogs(LogSearchRequest logSearchRequest, int page, int size);


}
