SELECT *
FROM (
select
    iin_bin,

    toDate('2024-01-01') as date,

    concat('FNO240') as database,
    concat('Иные имущества') as aktivy,
    concat('Наличие') as oper,
    concat('Имущества за пределами РК- Кол-во имуществ:',kolichestv_imushestv_za_predelami_rk,'; Наименование и коды стран:', vse_vidy_imuchestva_i_kod_stran,';') as dopinfo,
    toInt32(concat('0')) as summ
from pfr_dashboard.fno_240_2023_24 where vse_vidy_imuchestva_i_kod_stran is not null
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';