package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.MainRequest;
import org.info.infobaza.dto.response.relation.RelationActive;
import org.info.infobaza.dto.response.relation.RelationActiveWithTypes;
import org.info.infobaza.service.relations.RelationService;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/relation")
public class RelationController {
    private final RelationService relationService;

    @LogRequest
    @PostMapping("/primary")
    @Cacheable(value = "primaryRelations", keyGenerator = "requestKeyGenerator")
    public ResponseEntity<RelationActiveWithTypes> allPrimaryRelationsOfPerson(@RequestBody MainRequest mainRequest) throws IOException {
        return ResponseEntity.ok().body(relationService.getPrimaryRelationsOfPerson(
                mainRequest.getIin_bin(),
                mainRequest.getDateFrom().toString(),
                mainRequest.getDateTo().toString()));
    }

    @LogRequest
    @PostMapping("/secondary")
    @Cacheable(value = "secondaryRelations", keyGenerator = "requestKeyGenerator")
    public ResponseEntity<?> allSecondaryRelationsOfPerson(@RequestBody MainRequest mainRequest) throws IOException {
        RelationActiveWithTypes active = relationService.getSecondaryRelationsOfPerson(
                mainRequest.getIin_bin(),
                mainRequest.getDateFrom().toString(),
                mainRequest.getDateTo().toString());
        return ResponseEntity.ok().body(active);
    }
}
