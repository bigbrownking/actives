package org.info.infobaza.util.convert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.info.RelationRecord;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.service.central_depository.CentralDepositaryService;
import org.info.infobaza.service.culs.CULSService;
import org.info.infobaza.service.eias.EIASService;
import org.info.infobaza.service.enis_notarius.EnisNotariusService;
import org.info.infobaza.service.enpf.ENPFService;
import org.info.infobaza.service.esf.ESFService;
import org.info.infobaza.service.fm.FMService;
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
    private final ESFService esfService;
    private final ENPFService enpfService;
    private final KGDMFRKService kgdmfrkService;
    private final FMService fmService;
    private final FNOService fnoService;
    private final FNO200_05Service fno20005Service;
    private final FNO240Service fno240Service;
    private final FNO250Service fno250Service;
    private final FNO270Service fno270Service;
    private final GKBAUTOService gkbautoService;
    private final EnisNotariusService enisNotariusService;
    private final KAPMVDAUTOService kapmvdautoService;
    private final NaoConService naoConService;
    private final MinTransportService minTransportService;
    private final MCXService mcxService;
    private final CentralDepositaryService centralDepositaryService;
    private final CULSService culsService;
    private final EIASService eiasService;
    private final MoneyService moneyService;

    public void fetchAllActives(String iin,
                                 String dateFrom, String dateTo,
                                 List<String> sources,
                                 List<InformationRecordDt> allInfoRecords, List<ESFInformationRecordDt> allEsfRecords) throws IOException {
        try {
            if (sources == null || sources.isEmpty() || sources.contains("Ценные бумаги")) {
                allInfoRecords.addAll(keepDistinctInfo(centralDepositaryService.getCDPaper(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(fmService.getFMPaper(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Земельный участок")) {
                allInfoRecords.addAll(keepDistinctInfo(culsService.getCULSGround(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Недвижимое имущество")) {
                allInfoRecords.addAll(keepDistinctInfo(culsService.getCULSHouse(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(fmService.getFMHouse(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(fno250Service.getFNO250House(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270House(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Транспортные средства")) {
                allInfoRecords.addAll(keepDistinctInfo(culsService.getCULSTransport(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(fno250Service.getFNO250Transport(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(gkbautoService.getGKBAuto(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(kapmvdautoService.getKapMvdAuto(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("ЕНИС НОТАРИУС")) {
                allInfoRecords.addAll(keepDistinctInfo(enisNotariusService.getAllInformation(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Цифровые активы")) {
                allInfoRecords.addAll(keepDistinctInfo(fmService.getFMActiv(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Сейфовые ячейки")) {
                allInfoRecords.addAll(keepDistinctInfo(fmService.getFMSafe(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Денежные средства")) {
                allInfoRecords.addAll(keepDistinctInfo(fmService.getFMMoney(iin, dateFrom, dateTo)));
                allEsfRecords.addAll(keepDistinctEsf(moneyService.getAllMoneyInfo(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Прочие активы")) {
                allInfoRecords.addAll(keepDistinctInfo(fno240Service.getFNO240Other(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(fno240Service.getFNO240Other2(iin, dateFrom, dateTo)));

                allInfoRecords.addAll(keepDistinctInfo(fno250Service.getFNO250Other(iin, dateFrom, dateTo)));
                allEsfRecords.addAll(keepDistinctEsf(fno250Service.getFNO250Other2(iin, dateFrom, dateTo)));

                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270Other(iin, dateFrom, dateTo)));
                allInfoRecords.addAll(keepDistinctInfo(fno270Service.getFNO270Other2(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("ЮЛ")) {
                allInfoRecords.addAll(keepDistinctInfo(kgdmfrkService.getKgdUl(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Спецтехника")) {
                allInfoRecords.addAll(keepDistinctInfo(mcxService.getMcxSpecialTech(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Воздушные судна")) {
                allInfoRecords.addAll(keepDistinctInfo(minTransportService.getAirPlane(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Водный транспорт")) {
                allInfoRecords.addAll(keepDistinctInfo(minTransportService.getWaterTransport(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("ЖД составы")) {
                allInfoRecords.addAll(keepDistinctInfo(minTransportService.getMinTransTrain(iin, dateFrom, dateTo)));
            }
            fetchAllESFInfo(iin, dateFrom, dateTo, sources, allEsfRecords);

        } catch (IOException e) {
            log.error("Error fetching FM records from FMService for IIN: {}", iin, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching FM records from FMService for IIN: {}", iin, e);
        }
    }
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
    public void fetchAllESFInfo(String iin,
                                 String dateFrom, String dateTo,
                                 List<String> sources,
                                 List<ESFInformationRecordDt> allEsfRecords) throws IOException {
        try {
            if (sources == null || sources.isEmpty() || sources.contains("Животные")) {
                allEsfRecords.addAll(keepDistinctEsf(esfService.getESFAnimals(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Недвижимое имущество")) {
                allEsfRecords.addAll(keepDistinctEsf(esfService.getESFHouse(iin, dateFrom, dateTo)));
                allEsfRecords.addAll(keepDistinctEsf(eiasService.getEiasHouse(iin, dateFrom, dateTo)));
                allEsfRecords.addAll(keepDistinctEsf(naoConService.getNaoConHouse(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Сейфовые ячейки")) {
                allEsfRecords.addAll(keepDistinctEsf(esfService.getESFSafe(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Украшения и золото")) {
                allEsfRecords.addAll(keepDistinctEsf(esfService.getESFJewelry(iin, dateFrom, dateTo)));
                allEsfRecords.addAll(keepDistinctEsf(eiasService.getEiasJewelry(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Предметы исскуства")) {
                allEsfRecords.addAll(keepDistinctEsf(esfService.getESFPicture(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Прочие активы")) {
                allEsfRecords.addAll(keepDistinctEsf(esfService.getESFTur(iin, dateFrom, dateTo)));
                allEsfRecords.addAll(keepDistinctEsf(esfService.getESFOther(iin ,dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Транспортные средства")) {
                allEsfRecords.addAll(keepDistinctEsf(esfService.getESFTransport(iin, dateFrom, dateTo)));
                allEsfRecords.addAll(keepDistinctEsf(eiasService.getEiasTransport(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Ценные бумаги")) {
                allEsfRecords.addAll(keepDistinctEsf(eiasService.getEiasPaper(iin, dateFrom, dateTo)));
                allEsfRecords.addAll(keepDistinctEsf(fno250Service.getFNO250Paper(iin, dateFrom, dateTo)));
            }
            if (sources == null || sources.isEmpty() || sources.contains("Ценные активы")) {
                allEsfRecords.addAll(keepDistinctEsf(eiasService.getEiasActives(iin, dateFrom, dateTo)));
            }
        } catch (IOException e) {
            log.error("Error fetching ESF records from ESFService for IIN: {}", iin, e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error fetching ESF records from ESFService for IIN: {}", iin, e);
        }
    }


    public static List<InformationRecordDt> keepDistinctInfo(List<InformationRecordDt> informationRecords) {
        return informationRecords.stream().distinct().collect(Collectors.toList());
    }

    public static List<ESFInformationRecordDt> keepDistinctEsf(List<ESFInformationRecordDt> esfInformationRecords) {
        return esfInformationRecords.stream().distinct().collect(Collectors.toList());
    }
    public static List<RelationRecord> keepDistinctRelations(List<RelationRecord> relationRecords) {
        return relationRecords.stream().distinct()
                .collect(Collectors.toList());
    }
}
