SELECT
    iin_bin,
    iin_bin_pokup,
    iin_bin_prod,
    date,
    database,
    aktivy,
    oper,
    dopinfo,
    num_doc,
    summ
FROM (
    -- Query 1: Имущества за пределами РК
    select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,

    concat('FNO250') as database,
    concat('Прочие активы') as aktivy,
    concat('Наличие') as oper,
    concat('Имущества за пределами РК- Кол-во имуществ:',kolichestvo_nedvij_imushestv_zaregan_inos_gos,'; Наименование и коды стран:', vse_vidy_nedvij_imushestv_i_kod_stran,';') as dopinfo,
    concat('') as num_doc,
    toInt32(concat('0')) as summ
from pfr_dashboard.fno_250_2023_24 where vse_vidy_nedvij_imushestv_i_kod_stran is not null


    UNION ALL

    -- Query 2: Имущества переданные в дов.управление
    select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Прочие активы') as aktivy,
    concat('Наличие') as oper,
    concat('Имущества переданные в дов.управление:',imushestva_peredannye_na_doveritelnoe_upr,';') as dopinfo,
    concat('') as num_doc,
    toInt32(0) as summ

from pfr_dashboard.fno_250_2023_24 where imushestva_peredannye_na_doveritelnoe_upr is not null

    UNION ALL

    -- Query 3: Инвест.золото
    select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Прочие активы') as aktivy,
    concat('Наличие') as oper,
    concat('Инвест.золото- Сумма:',summa_izvesticionnogo_zolota,';') as dopinfo,
    concat('') as num_doc,
    toInt32(toFloat32(summa_izvesticionnogo_zolota)) as summ

from pfr_dashboard.fno_250_2023_24 where toInt32(toFloat32(summa_izvesticionnogo_zolota))>0

    UNION ALL

    -- Query 4: Интеллектуальная собственность
    select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Прочие активы') as aktivy,
    concat('Наличие') as oper,
    concat('Интеллектуальная собственность- Патент:',patent,';') as dopinfo,
    concat('') as num_doc,
    toInt32(0) as summ

from pfr_dashboard.fno_250_2023_24 where patent is not null


    UNION ALL

    -- Query 5: Кол-во ЮЛ
    select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Прочие активы') as aktivy,
    concat('Наличие') as oper,
    concat('Кол-во ЮЛ:',kolichestv_svedeniy_o_ul,'; Наименование и коды стран:', vse_naimenovanye_ul_i_kody_stran,';') as dopinfo,
    concat('') as num_doc,
    toInt32(0) as summ

from pfr_dashboard.fno_250_2023_24 where vse_naimenovanye_ul_i_kody_stran is not null

) AS combined
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO'