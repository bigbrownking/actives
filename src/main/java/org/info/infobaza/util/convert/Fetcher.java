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

}
