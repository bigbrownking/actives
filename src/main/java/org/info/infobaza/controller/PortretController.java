package org.info.infobaza.controller;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.FindCarRequest;
import org.info.infobaza.dto.request.FindHouseRequest;
import org.info.infobaza.dto.request.GetPortretRequest;
import org.info.infobaza.dto.response.info.car.CarResponse;
import org.info.infobaza.dto.response.info.house.HousePortret;
import org.info.infobaza.dto.response.person.Person;
import org.info.infobaza.exception.NotFoundException;
import org.info.infobaza.service.ObjectFinder;
import org.info.infobaza.service.portret.PortretService;
import org.info.infobaza.util.logging.LogRequest;
import org.springframework.cache.annotation.Cacheable;
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
    private final ObjectFinder objectFinder;

    @LogRequest
    @PostMapping("/person")
    public ResponseEntity<Person> getPortret(@RequestBody GetPortretRequest byIINRequest) throws IOException, NotFoundException {
        return ResponseEntity.ok(portretService.getPerson(
                byIINRequest.getIin()
        ));
    }
    @LogRequest
    @PostMapping("/car")
    @Cacheable(value = "car", key = "#request")
    public ResponseEntity<CarResponse> getCarInfo(
            @RequestBody FindCarRequest request) throws IOException {
        return ResponseEntity.ok(objectFinder.getCar(request));
    }

    @LogRequest
    @PostMapping("/house")
    @Cacheable(value = "house", key = "#request")
    public ResponseEntity<HousePortret> getHouseInfo(
            @RequestBody FindHouseRequest request) throws IOException {
        return ResponseEntity.ok(objectFinder.getHouse(request));
    }
}
