package org.info.infobaza.service;

import lombok.RequiredArgsConstructor;
import org.info.infobaza.constants.QueryLocationDictionary;
import org.info.infobaza.model.info.active_income.ESFInformationRecordDt;
import org.info.infobaza.model.info.active_income.InformationRecordDt;
import org.info.infobaza.model.info.active_income.NaoConRecordDt;
import org.info.infobaza.util.convert.Mapper;
import org.info.infobaza.util.convert.SQLFileUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class FetcherFactory {
    private final JdbcTemplate jdbcTemplate;
    private final SQLFileUtil sqlFileUtil;
    private final Mapper mapper;

    @Bean
    public List<DataFetcher> allDataFetchers() {
        List<DataFetcher> fetchers = new ArrayList<>();

        // ==================== ESF SERVICE ====================
        fetchers.add(createESFFetcher(
                new String[]{"Реализация", "Приобретение"},
                "ESF",
                new String[]{"Транспортные средства"},
                QueryLocationDictionary.ESF_Транспортные_средства
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Приобретение", "Реализация"},
                "ESF",
                new String[]{"Недвижимое имущество"},
                QueryLocationDictionary.ESF_Недвижимое_имущество
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Приобретение", "Реализация"},
                "ESF",
                new String[]{"Украшения и золото"},
                QueryLocationDictionary.ESF_Украшения_и_золото
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Приобретение"},
                "ESF",
                new String[]{"Прочие активы"},
                QueryLocationDictionary.ESF_Прочие_активы
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Приобретение"},
                "ESF",
                new String[]{"Турпакеты", "Прочие активы"},
                QueryLocationDictionary.ESF_Турпакеты
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Реализация", "Приобретение"},
                "ESF",
                new String[]{"Животные"},
                QueryLocationDictionary.ESF_Животные
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Приобретение", "Реализация"},
                "ESF",
                new String[]{"Предметы исскуства"},
                QueryLocationDictionary.ESF_Предметы_исскуства
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "ESF",
                new String[]{"Сейфовые ячейки"},
                QueryLocationDictionary.ESF_Сейфовые_ячейки
        ));

        // ==================== EIAS SERVICE ====================
        fetchers.add(createESFFetcher(
                new String[]{"Приобретение", "Реализация"},
                "ЕИАС",
                new String[]{"Недвижимое имущество"},
                QueryLocationDictionary.ЕИАС_Недвижимое_имущество
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "ЕИАС",
                new String[]{"Денежные средства"},
                QueryLocationDictionary.ЕИАС_Денежные_средства,
                true, false
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "ЕИАС",
                new String[]{"Сейфовые ячейки"},
                QueryLocationDictionary.ЕИАС_Сейфовые_ячейки,
                true, false
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Приобретение"},
                "ЕИАС",
                new String[]{"Транспортные средства"},
                QueryLocationDictionary.ЕИАС_Транспортные_средства
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Реализация"},
                "ЕИАС",
                new String[]{"Украшения и золото"},
                QueryLocationDictionary.ЕИАС_Украшения_и_золото
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Приобретение", "Реализация"},
                "ЕИАС",
                new String[]{"Ценные бумаги"},
                QueryLocationDictionary.ЕИАС_Ценные_бумаги
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Приобретение", "Реализация", "Наличие"},
                "ЕИАС",
                new String[]{"Цифровые активы"},
                QueryLocationDictionary.ЕИАС_Цифровые_активы
        ));

        // ==================== CULS SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "ЦУЛС",
                new String[]{"Транспортные средства"},
                QueryLocationDictionary.ЦУЛС_Транспортные_средства,
                true, false
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "ЦУЛС",
                new String[]{"Земельный участок"},
                QueryLocationDictionary.ЦУЛС_Земельный_участок,
                true, false
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "ЦУЛС",
                new String[]{"Недвижимое имущество"},
                QueryLocationDictionary.ЦУЛС_Недвижимое_имущество,
                true, false
        ));

        // ==================== CENTRAL DEPOSITARY SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "Центральный депозитарий",
                new String[]{"Ценные бумаги"},
                QueryLocationDictionary.Центральный_депозитарий_Ценные_бумаги,
                true, false
        ));

        // ==================== FNO250 SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "FNO250",
                new String[]{"Иные имущества"},
                QueryLocationDictionary.FNO250_Иные_имущества,
                true, false
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO250",
                new String[]{"Прочие активы"},
                QueryLocationDictionary.FNO250_Прочие_активы
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO250",
                new String[]{"Недвижимое имущество"},
                QueryLocationDictionary.FNO250_Недвижимое_имущество
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO250",
                new String[]{"Ценные бумаги"},
                QueryLocationDictionary.FNO250_Ценные_бумаги
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO250",
                new String[]{"Транспортные средства"},
                QueryLocationDictionary.FNO250_Транспортные_средства
        ));

        // ==================== FNO270 SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Приобретение", "Реализация"},
                "FNO270",
                new String[]{"Недвижимое имущество"},
                QueryLocationDictionary.FNO270_Недвижимое_имущество,
                true, false
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO270",
                new String[]{"Иные имущества"},
                QueryLocationDictionary.FNO270_Иные_имущества
        ));

        // FNO270 Income methods
        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO270",
                new String[]{"Доход от осуществления нотариуса, судебного исполнителя, адвоката, профессионального медиатора"},
                QueryLocationDictionary.FNO270_Доход_от_осуществления_нотариуса_судебного_исполнителя_адвоката_профессионального_медиатора,
                false, true
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO270",
                new String[]{"в. т. ч. Доход от ИП"},
                QueryLocationDictionary.FNO270_в_т_ч_Доход_от_ИП,
                false, true
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO270",
                new String[]{"в. т. ч. Доход из источников за пределами РК"},
                QueryLocationDictionary.FNO270_в_т_ч_Доход_из_источников_за_пределами_РК,
                false, true
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO270",
                new String[]{"в.т.ч. Доход лица занимающиеся частной практикой"},
                QueryLocationDictionary.FNO270_в_т_ч_Доход_лица_занимающиеся_частной_практикой,
                false, true
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO270",
                new String[]{"в.т.ч. Имущественный доход"},
                QueryLocationDictionary.FNO270_в_т_ч_Имущественный_доход,
                false, true
        ));


        // ==================== FNO240 SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO240",
                new String[]{"Прибыль КИК"},
                QueryLocationDictionary.FNO240_Прибыль_КИК,
                false, true
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO240",
                new String[]{"Иные имущества"},
                QueryLocationDictionary.FNO240_Иные_имущества
        ));

        // ==================== ENPF SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{},
                "ЕНПФ",
                new String[]{"Доход по данным ЕНПФ"},
                QueryLocationDictionary.ЕНПФ_Доход_по_данным_ЕНПФ,
                false, true
        ));

        // ==================== FNO SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO",
                new String[]{"Доход ИП"},
                QueryLocationDictionary.FNO_Доход_ИП,
                false, true
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO",
                new String[]{"СГД ЮЛ"},
                QueryLocationDictionary.FNO_СГД_ЮЛ,
                false, true
        ));

        // ==================== FNO200_05 SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{},
                "FNO200_05",
                new String[]{"Доход по данным ФНО"},
                QueryLocationDictionary.FNO200_05_Доход_по_данным_ФНО,
                false, true
        ));

        // ==================== GKB AUTO SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "ГКБ Cведения по страховке Авто",
                new String[]{"Транспортные средства", "ГКБ-Транспортные средства"},
                QueryLocationDictionary.ГКБ_Cведения_по_страховке_Авто_Транспортные_средства,
                true, false
        ));

        // ==================== KAP MVD AUTO SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "КАП МВД Cведения по владельцам Авто",
                new String[]{"Транспортные средства"},
                QueryLocationDictionary.КАП_МВД_Cведения_по_владельцам_Авто_Транспортные_средства,
                true, false
        ));

        // ==================== KGD MF RK SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "Ведения КГД МФ РК",
                new String[]{"ЮЛ"},
                QueryLocationDictionary.СВедения_КГД_МФ_РК_ЮЛ,
                true, false
        ));

        // ==================== MCX SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Реализация", "Приобретение"},
                "Сведения МСХ",
                new String[]{"Спецтехника"},
                QueryLocationDictionary.Сведения_МСХ_Спецтехника,
                true, false
        ));

        // ==================== MIN TRANSPORT SERVICE ====================
        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "Сведения Минтранспорта",
                new String[]{"ЖД составы"},
                QueryLocationDictionary.Сведения_Минтранспорта_ЖД_составы,
                true, false
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "Сведения Минтранспорта",
                new String[]{"Воздушные судна"},
                QueryLocationDictionary.Сведения_Минтранспорта_Воздушные_судна,
                true, false
        ));

        fetchers.add(createSimpleFetcher(
                new String[]{"Наличие"},
                "Сведения Минтранспорта",
                new String[]{"Водный транспорт"},
                QueryLocationDictionary.Сведения_Минтранспорта_Водный_транспорт,
                true, false
        ));

        // ==================== MONEY SERVICE ====================
        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO270",
                new String[]{"Денежные средства"},
                QueryLocationDictionary.Money_money
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO240",
                new String[]{"Денежные средства"},
                QueryLocationDictionary.Money_money
        ));

        fetchers.add(createESFFetcher(
                new String[]{"Наличие"},
                "FNO250",
                new String[]{"Денежные средства"},
                QueryLocationDictionary.Money_money
        ));

        // ==================== NAO CON SERVICE ====================
        fetchers.add(createNaoConFetcher(
                new String[]{"Реализация", "Приобретение"},
                new String[]{"Недвижимое имущество"}
        ));

        return fetchers;
    }

    private DataFetcher createESFFetcher(String[] types, String source, String[] vids,
                                         QueryLocationDictionary queryPath) {
        FetcherConfig config = new FetcherConfig(types, source, vids, queryPath,
                true, false, ESFInformationRecordDt.class);
        return new ESFDataFetcher(jdbcTemplate, sqlFileUtil, mapper, config);
    }

    private DataFetcher createSimpleFetcher(String[] types, String source, String[] vids,
                                            QueryLocationDictionary queryPath,
                                            boolean isActive, boolean isIncome) {
        FetcherConfig config = new FetcherConfig(types, source, vids, queryPath,
                isActive, isIncome, InformationRecordDt.class);
        return new SimpleDataFetcher(jdbcTemplate, sqlFileUtil, mapper, config);
    }

    private DataFetcher createNaoConFetcher(String[] types, String[] vids) {
        FetcherConfig config = new FetcherConfig(types, "НАО ЦОН", vids,
                QueryLocationDictionary.НАО_ЦОН_Недвижимое_имущество,
                true, false, NaoConRecordDt.class);
        return new NaoConDataFetcher(jdbcTemplate, sqlFileUtil, mapper, config);
    }
}