SELECT *
FROM (
select
    iin_bin,

    toDate('2024-01-01') as date,

    concat('FNO240') as database,
    concat('Прибыль КИК') as oper,
    concat('Сумма дохода:',sum_pribyl_kik_i_postoyannogo_uchr_kik,';') as dopinfo,
    toInt32(toFloat32(sum_pribyl_kik_i_postoyannogo_uchr_kik)) as summ
from pfr_dashboard.fno_240_2023_24 where toInt32(toFloat32(sum_pribyl_kik_i_postoyannogo_uchr_kik))>1 and toInt32(toFloat32(sum_pribyl_kik_i_postoyannogo_uchr_kik))<100000000000
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';