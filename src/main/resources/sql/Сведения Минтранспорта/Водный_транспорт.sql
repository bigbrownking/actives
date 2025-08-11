SELECT *
FROM (
select
    iin_bin,

    toDate(concat('2024-01-01')) as date,

    concat('Сведения Минтранспорта') as database,
    concat('Водный транспорт') as aktivy,
    concat('Наличие') as oper,
    concat('Тип судна:',coalesce(type_sudna, '0'),'; Название судна:',coalesce(nazivanye_sudna, '0'),';') as dopinfo,
    toInt32(concat('0')) as summ
from pfr_dashboard.reestr_vodniy_sudna
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';