package org.info.infobaza.util.convert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.person.RelationRecord;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.service.central_depository.CentralDepositaryService;
import org.info.infobaza.service.culs.CULSService;
import org.info.infobaza.service.eias.EIASService;
import org.info.infobaza.service.enpf.ENPFService;
import org.info.infobaza.service.esf.ESFService;
import org.info.infobaza.service.fno.FNOService;
import org.info.infobaza.service.fno_200_05.FNO200_05Service;
import org.info.infobaza.service.fno_240.FNO240Service;
import org.info.infobaza.service.fno_250.FNO250Service;
import org.info.infobaza.service.fno_270.FNO270Service;
import org.info.infobaza.service.gkb_auto.GKBAUTOService;
import org.info.infobaza.service.kap_mvd_auto.KAPMVDAUTOService;
import org.info.infobaza.service.kgd_mf_rk.KGDMFRKService;
import org.info.infobaza.service.mcx.MCXService;
import org.info.infobaza.service.min_transport.MinTransportService;
import org.info.infobaza.service.money.MoneyService;
import org.info.infobaza.service.nao_con.NaoConService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j
@RequiredArgsConstructor
public class Fetcher {
    private final ENPFService enpfService;
    private final FNOService fnoService;
    private final FNO200_05Service fno20005Service;
    private final FNO240Service fno240Service;
    private final FNO270Service fno270Service;

    public void fetchAllIncomes(String iin,
                                 String dateFrom, String dateTo,
                                 List<InformationRecordDt> allInfoRecords, List<String> sources) throws IOException {
        try {
            if (sources == null || sources.isEmpty() || sources.contains("Доход по данным ЕНПФ")) {
                allInfoRecords.addAll(keepDistinctInfo(enpfService.getENPF(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Доход ИП")) {
                allInfoRecords.addAll(keepDistinctInfo(fnoService.getFNOIncomeIP(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("СГД ЮЛ")) {
                allInfoRecords.addAll(keepDistinctInfo(fnoService.getFNOSGD_UL(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Доход по данным ФНО")) {
                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270Income(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(fno20005Service.getFNO200_05FNO(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Прибыль КИК")) {
                allInfoRecords.addAll(keepDistinctInfo(fno240Service.getFNO240IncomeKik(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("в.т.ч. Доход от ИП")) {
                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270IncomeIP(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("в.т.ч. Доход из источников за пределами РК")) {
                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270IncomeAbroad(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("в.т.ч. Имущественный доход")) {
                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270IncomeItems(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Доход от осуществления нотариуса, судебного исполнителя, адвоката, профессионального медиатора")) {
                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270IncomeNotarius(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("в.т.ч. Доход лица занимающиеся частной практикой")) {
                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270IncomePractice(iin, dateFrom, dateTo)));
            }

        } catch (IOException e) {
            log.error("Error fetching income records for IIN: {}", iin, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching income records for IIN: {}", iin, e);
        }
    }
    public static List<InformationRecordDt> keepDistinctInfo(List<InformationRecordDt> informationRecords) {
        return informationRecords.stream().distinct().collect(Collectors.toList());
    }

    public static List<ESFInformationRecordDt> keepDistinctEsf(List<ESFInformationRecordDt> esfInformationRecords) {
        return esfInformationRecords.stream().distinct().collect(Collectors.toList());
    }
    public static List<RelationRecord> keepDistinctRelations(List<RelationRecord> relationRecords) {
        return relationRecords.stream().distinct().collect(Collectors.toList());
    }
}
