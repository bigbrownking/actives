SELECT *
FROM (
select
    iin_zastrahovanny as iin_bin,
    toDate(start_date) as date,
    concat('ГКБ-Cведения по страховке Авто') as database,
    concat('Транспортные средства') as aktivy,
    concat('Наличие') as oper,
    concat('Авто:',coalesce(marka, '0'),' ',model,'; ИИН страхователя:',coalesce(iin_strahovatel, '0'),';') as dopinfo,
    toInt32(concat('0')) as summ
from pfr_dashboard.auto_vin

union all



select
    iin_strahovatel as iin_bin,
    toDate(start_date) as date,
    concat('Cведения по страховке Авто') as database,
    concat('ГКБ-Транспортные средства') as aktivy,
    concat('Наличие') as oper,
    concat('Авто:',coalesce(marka, '0'),' ',model,'; ИИН застрахованного:',coalesce(iin_zastrahovanny, '0'),';') as dopinfo,
    toInt32(concat('0')) as summ
from pfr_dashboard.auto_vin

) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';