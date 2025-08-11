SELECT *
FROM (
select
    iin_analiziruemyi as iin_bin,
    toDate('2024-01-01') as date,
    concat('FNO') as database,
    concat('Доход ИП') as oper,
    concat('220_00_015:',f_220_00_015,'; 910_00_001:',  f_910_00_001, '; 911_00_001:',  f_911_00_001, '; 912_00_004:',  f_912_00_004, '; 913_00_001:',  f_913_00_001,'; 920_00_001:',  f_920_00_001,  ';') as dopinfo,
    toInt32(toFloat32(dohot_ip)) as summ


    
from pfr_dashboard.dohot_ip where toInt32(toFloat32(dohot_ip))>0
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';