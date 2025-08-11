SELECT *
FROM (
select

    zaiyavitel_iin_bin as iin_bin,

    toDate(concat('2024-01-01')) as date,

    concat('Сведения Минтранспорта') as database,
    concat('ЖД составы') as aktivy,
    concat('Наличие') as oper,
    concat('Тип жд состава:',coalesce(type_podvijnogo_sostava, '0'),'; Категория жд состава:', coalesce(categorya_podvijnogo_sostava, '0'),';') as dopinfo,
    toInt32(concat('0')) as summ
from pfr_dashboard.reestr_jd)
 AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';