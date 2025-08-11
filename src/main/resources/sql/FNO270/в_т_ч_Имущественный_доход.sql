SELECT *
FROM (
select
    iin_bin,
    
    toDate('2024-01-01') as date,
    
    concat('FNO270') as database,
    concat('в.т.ч. Имущественный доход') as oper,
    concat('Сумма дохода:',vtch_imuchestvenny_dohot,';') as dopinfo,
    toInt32(toFloat32(vtch_imuchestvenny_dohot)) as summ
from pfr_dashboard.fno_270_2023_24 where toInt32(toFloat32(vtch_imuchestvenny_dohot))>1 and toInt32(toFloat32(vtch_imuchestvenny_dohot))<100000000000
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';