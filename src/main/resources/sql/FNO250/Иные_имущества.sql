SELECT *
FROM (
select
    iin_bin,

    toDate('2024-01-01') as date,

    concat('FNO250') as database,
    concat('Иные имущества') as aktivy,
    concat('Наличие') as oper,
    concat('Имущества за пределами РК- Кол-во имуществ:',kolichestvo_nedvij_imushestv_zaregan_inos_gos,'; Наименование и коды стран:', vse_vidy_nedvij_imushestv_i_kod_stran,';') as dopinfo,
    toInt32(concat('0')) as summ
from pfr_dashboard.fno_250_2023_24 where vse_vidy_nedvij_imushestv_i_kod_stran is not null
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';