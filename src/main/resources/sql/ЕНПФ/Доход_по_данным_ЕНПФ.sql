SELECT *
FROM (
select
    distinct
    IIN as iin_bin,
    toDate(PAY_DATE) as date,

    concat('ЕНПФ') as database,
    concat('Доход по данным ЕНПФ') as oper,

    concat('Место работы:',P_RNN,';') as dopinfo,
    AMOUNT*10 as summ
from pfr_dashboard.imp_pension_fl_contr where KNP='010'
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';