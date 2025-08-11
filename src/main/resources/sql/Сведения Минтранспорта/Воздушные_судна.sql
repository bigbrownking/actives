SELECT *
FROM (
select
    iin_bin_sobst_1 as iin_bin,

    toDate(concat('2024-01-01')) as date,

    concat('Сведения Минтранспорта') as database,
    concat('Воздушные судна') as aktivy,
    concat('Наличие') as oper,
    concat('Тип ВС:',type_vs,'; Борт номер:',bort_nomer,';') as dopinfo,
    toInt32(concat('0')) as summ
from pfr_dashboard.grajd_vozdush_sudna
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';