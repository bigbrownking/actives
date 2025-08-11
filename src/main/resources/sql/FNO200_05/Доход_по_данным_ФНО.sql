 SELECT *
FROM (
 select

    field_200_05_C as iin_bin,

    case
    when period_quarter = 1 and period_year = 2023 then date('2023-01-01')
    when period_quarter = 2 and period_year = 2023 then date('2023-04-01')
    when period_quarter = 3 and period_year = 2023 then date('2023-07-01')
    when period_quarter = 4 and period_year = 2023 then date('2023-10-01')
    when period_quarter = 1 and period_year = 2024 then date('2024-01-01')
    when period_quarter = 2 and period_year = 2024 then date('2024-04-01')
    when period_quarter = 3 and period_year = 2024 then date('2024-07-01')
    else date('9999-99-99') end as date,
    concat('FNO200_05') as database,
    concat('Доход по данным ФНО') as oper,
    concat('Место работы:',iin_bin,';') as dopinfo,
    field_200_05_S as summ

from pfr_dashboard.fno_200_05 where field_200_05_C is not null and field_200_05_S>0
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';