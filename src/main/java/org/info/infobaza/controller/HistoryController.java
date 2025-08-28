package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.HistoryRequest;
import org.info.infobaza.model.main.Request;
import org.info.infobaza.service.history.HistoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/history/")
@RequiredArgsConstructor
public class HistoryController {
    private final HistoryService historyService;

    @PostMapping("/recent")
    private ResponseEntity<Page<Request>> getRecentRequestsByUser(@RequestBody HistoryRequest historyRequest,
                                                                  @RequestParam(defaultValue = "0") Integer page,
                                                                  @RequestParam(defaultValue = "10") Integer size){
        return ResponseEntity.ok(historyService.getRecentRequests(
                historyRequest,
                page,
                size
        ));
    }
}
