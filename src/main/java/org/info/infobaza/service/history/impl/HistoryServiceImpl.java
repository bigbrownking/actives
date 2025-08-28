package org.info.infobaza.service.history.impl;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.dto.request.HistoryRequest;
import org.info.infobaza.model.main.Request;
import org.info.infobaza.repository.main.RequestRepository;
import org.info.infobaza.service.history.HistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.info.infobaza.util.convert.JpaPageable.createPageableSorted;
import static org.info.infobaza.util.user.UserUtil.*;

@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {
    private final RequestRepository requestRepository;

    @Override
    public Page<Request> getRecentRequests(HistoryRequest historyRequest, int page, int size) {

        long userId = getCurrentUser().getId();
        Pageable pageable = createPageableSorted(page, size, Sort.by("timestamp").descending());

        String iin = historyRequest.getIinBin();
        LocalDate dateFrom = historyRequest.getDateFrom();
        LocalDate dateTo = historyRequest.getDateTo();

        return requestRepository.getRequestsBy(
                userId, iin, dateFrom, dateTo, pageable);
    }

    @Override
    public Request createRequest(Request request) {
        return requestRepository.save(Request.builder()
                .user(getCurrentUser())
                .iinBin(request.getIinBin())
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .clientIp(getClientIpAddress(getCurrentHttpRequest()))
                .timestamp(LocalDateTime.now())
                .years(request.getYears())
                .vids(request.getVids())
                .types(request.getTypes())
                .sources(request.getSources())
                .iins(request.getIins())
                .build());
    }

    @Override
    public Page<Request> allUsersRequests(int page, int size) {
        return requestRepository.findAll(
                createPageableSorted(
                        page, size,
                        Sort.by("timestamp").descending()));
    }
}