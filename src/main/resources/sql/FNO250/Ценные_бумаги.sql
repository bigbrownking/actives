SELECT
    iin_bin,
    iin_bin_pokup,
    iin_bin_prod,
    date,
    database,
    aktivy,
    oper,
    dopinfo,
    num_doc,
    summ
FROM (
    -- Query 1: Производные финансовые инструменты
    select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Ценные бумаги') as aktivy,
    concat('Наличие') as oper,
    concat('Производные финансовые инструменты- cумма;') as dopinfo,
    concat('') as num_doc,
    toInt32(toFloat32(obshaya_stoimost_cennyh_bumag_pfi)) as summ

from pfr_dashboard.fno_250_2023_24 where toInt32(toFloat32(obshaya_stoimost_cennyh_bumag_pfi))>0


    UNION ALL

    -- Query 2: Паи
    select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Ценные бумаги') as aktivy,
    concat('Наличие') as oper,
    concat('Паи- cумма пиф;') as dopinfo,
    concat('') as num_doc,
    toInt32(toFloat32(obshaya_stoimost_pif)) as summ

from pfr_dashboard.fno_250_2023_24 where toInt32(toFloat32(obshaya_stoimost_pif))>0
) AS combined
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO'