SELECT *
FROM (
select
    iin_bin,

    toDate('2024-01-01') as date,

    concat('FNO270') as database,
    concat('Недвижимое имущество') as aktivy,
    concat('Приобретение') as oper,
    concat('Стоимость приобретения имуществ') as dopinfo,
    toInt32(toFloat32(stoimost_priobretenye_imushestv)) as summ
from pfr_dashboard.fno_270_2023_24 where toInt32(toFloat32(stoimost_priobretenye_imushestv))>0

union all

select
    iin_bin,

    toDate('2024-01-01') as date,

    concat('FNO270') as database,
    concat('Недвижимое имущество') as aktivy,
    concat('Реализация') as oper,
    concat('Стоимость обчужденного имуществ') as dopinfo,
    toInt32(toFloat32(stoimost_obchujdennogo_imushestv)) as summ
from pfr_dashboard.fno_270_2023_24 where toInt32(toFloat32(stoimost_obchujdennogo_imushestv))>0
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';