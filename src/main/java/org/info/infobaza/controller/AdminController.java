package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.model.main.Request;
import org.info.infobaza.service.history.HistoryService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/")
@RequiredArgsConstructor
public class AdminController {
    private final HistoryService historyService;

    @GetMapping("/requests")
    public ResponseEntity<Page<Request>> allUsersRequests(@RequestParam(defaultValue = "0") Integer page,
                                                          @RequestParam(defaultValue = "10") Integer size){
        return ResponseEntity.ok(historyService.allUsersRequests(
                page, size
        ));
    }

    @DeleteMapping("/deleteRq")
    public ResponseEntity<?> deleteAllRequests(){
        historyService.deleteAllRequests();
        return ResponseEntity.ok("Deleted.");
    }
}
