SELECT *
FROM (
select

    founder_iin_bin as iin_bin,
    toDate('2025-01-01') as date,
    concat('Сведения КГД МФ РК') as database,
    concat('ЮЛ') as aktivy,
    concat('Наличие') as oper,
    concat('Намиенование ЮЛ: ',coalesce(taxpayer_name, '0'),'; Доля:', coalesce(share_percentage, '0'),'; БИН:', coalesce(taxpayer_iin_bin, '0'), ';') as dopinfo,
    toInt32(0) as summ

from pfr_dashboard.uchrediteli where founder_iin_bin is not null
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';