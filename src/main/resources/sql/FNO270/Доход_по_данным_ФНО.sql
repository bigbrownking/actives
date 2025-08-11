SELECT *
FROM (
select
    iin_bin,

    toDate('2024-01-01') as date,

    concat('FNO270') as database,
    concat('Доход по данным ФНО') as oper,
    concat('Сумма дохода:',dohot_po_dannym_fno_270_00,';') as dopinfo,
    toInt32(toFloat32(dohot_po_dannym_fno_270_00)) as summ
from pfr_dashboard.fno_270_2023_24 where toInt32(toFloat32(dohot_po_dannym_fno_270_00))>1 and  toInt32(toFloat32(dohot_po_dannym_fno_270_00))<100000000000
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';