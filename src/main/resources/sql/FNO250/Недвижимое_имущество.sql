SELECT *
FROM (
select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Недвижимое имущество') as aktivy,
    concat('Наличие') as oper,
    concat('Застройщик: ',zastroishik,';') as dopinfo,
    concat('') as num_doc,
    toInt32(toFloat32(obshaya_stoimost_ddu)) as summ

from pfr_dashboard.fno_250_2023_24 where toInt32(toFloat32(obshaya_stoimost_ddu))>0
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';
