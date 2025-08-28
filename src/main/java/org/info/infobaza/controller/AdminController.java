package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.LogSearchRequest;
import org.info.infobaza.model.info.log.Logs;
import org.info.infobaza.model.main.Request;
import org.info.infobaza.service.admin.AdminService;
import org.info.infobaza.service.history.HistoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;
    private final HistoryService historyService;

    @PostMapping("/searchLog")
    public ResponseEntity<Page<Logs>> searchFromLogs(@RequestBody LogSearchRequest logSearchRequest,
                                                     @RequestParam(defaultValue = "0") Integer page,
                                                     @RequestParam(defaultValue = "10") Integer size){
        return ResponseEntity.ok(adminService.searchFromLogs(
                logSearchRequest,
                page,
                size
        ));
    }

    @GetMapping("/requests")
    public ResponseEntity<Page<Request>> allUsersRequests(@RequestParam(defaultValue = "0") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size){
        return ResponseEntity.ok(historyService.allUsersRequests(
                page, size
        ));
    }



}
