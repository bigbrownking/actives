SELECT *
FROM (
select

    IIN_BIN as iin_bin,
    toDate(date_of_transfer_budget) as date,
    concat('ЦУЛС') as database,
    concat('Земельный участок') as aktivy,
    concat('Наличие') as oper,
    concat('КБК:',KBK,';') as dopinfo,
    toFloat32(replace((case when amount_to_be_calculated='' then '0' else amount_to_be_calculated end),',','.')) as summ
from pfr_dashboard.culs_uplata where  toFloat32(replace((case when amount_to_be_calculated='' then '0' else amount_to_be_calculated end),',','.'))>0 and (KBK = 104302 or KBK = 104309) and IIN_BIN is not null and IIN_BIN!=''
 ) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';