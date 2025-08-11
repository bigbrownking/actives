package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.GetPortretRequest;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/portret")
public class PortretController {
    private final PortretService portretService;


    @LogRequest
    @PostMapping("/person")
    public ResponseEntity<Person> getPortret(@RequestBody GetPortretRequest byIINRequest) throws IOException, NotFoundException {
        return ResponseEntity.ok().body(portretService.getPerson(
                byIINRequest.getIin()
        ));
    }

}
