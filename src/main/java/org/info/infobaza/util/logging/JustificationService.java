package org.info.infobaza.util.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.main.Log;
import org.info.infobaza.security.UserDetailsImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class JustificationService {
    private final LogService logService;

    private String placeApprovementText(String orderNum, String approvement_type,
                                        String caseNum,
                                        String orderDate,
                                        String articleName,
                                        String checkingName,
                                        String otherReasons,
                                        String organName,
                                        String sphereName,
                                        String tematikName,
                                        String rukName){
        List<String> approvements = new ArrayList<>();
        StringBuilder approvement_data = new StringBuilder();
        if (approvement_type != null && !approvement_type.isEmpty()) {
            approvements.add("ОСНОВАНИЕ ПРОВЕРКИ: " + approvement_type);
        }
        if (orderNum != null && !orderNum.isEmpty()) {
            approvements.add("НОМЕР ПРИКАЗА:" + orderNum);
        }
        if (caseNum != null && !caseNum.isEmpty()) {
            approvements.add("НОМЕР ДЕЛА: " + caseNum);
        }
        if (orderDate != null && !orderDate.isEmpty()) {
            approvements.add("ДАТА ПРИКАЗА: " + orderDate);
        }
        if (articleName != null && !articleName.isEmpty()) {
            approvements.add("НАЗВАНИЕ СТАТЬИ: " + articleName);
        }
        if (checkingName != null && !checkingName.isEmpty()) {
            approvements.add("ИМЯ ПРОВЕРЯЮЩЕГО: " + checkingName);
        }
        if (otherReasons != null && !otherReasons.isEmpty()) {
            approvements.add("ДРУГАЯ ПРИЧИНА: " + otherReasons);
        }
        if (organName != null && !organName.isEmpty()) {
            approvements.add("НАЗВАНИЕ ОРГАНА: " + organName);
        }
        if (sphereName != null && !sphereName.isEmpty()) {
            approvements.add("НАЗВАНИЕ СФЕРЫ: " + sphereName);
        }
        if (tematikName != null && !tematikName.isEmpty()) {
            approvements.add("НАЗВАНИЕ ТЕМАТИКИ: " + tematikName);
        }
        if (rukName != null && !rukName.isEmpty()) {
            approvements.add("ИМЯ РУКОВОДИТЕЛЯ: " + rukName);
        }
        for (String reason: approvements) {
            approvement_data.append(reason).append(", ");
        }
        return approvement_data.toString();
    }

    public boolean store(UserDetailsImpl userDetails, List<String> object,
                         String orderNum, String approvement_type,
                         String caseNum,
                         String orderDate,
                         String articleName,
                         String checkingName,
                         String otherReasons,
                         String organName,
                         String sphereName,
                         String tematikName,
                         String rukName, String activity){
        try {
            Log logg = new Log();
            String obwii = activity + object;
            logg.setObwii(obwii);
            String approvement_data = placeApprovementText(orderNum, approvement_type,
                    caseNum,
                    orderDate,
                    articleName,
                    checkingName,
                    otherReasons,
                    organName,
                    sphereName,
                    tematikName,
                    rukName);
            if(!userDetails.isAdmin() && approvement_data.isEmpty()){
                return false;
            }
            logg.setApprovementData(approvement_data);
            LocalDateTime current = LocalDateTime.now();
            logg.setUser(userDetails.getUserEntity());
            logg.setDate(current);
            logService.saveLog(logg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


}
