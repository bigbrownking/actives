SELECT *
FROM (
select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,

    concat('FNO270') as database,
    concat('Денежные средства') as aktivy,
    concat('Наличие') as oper,
    concat('Сумма на банковских счетах в иностранных банках:') as dopinfo,
    concat('') as num_doc,
    toInt32(toFloat32(summa_na_bankovskih_chetah_v_inos_bankah)) as summ
from pfr_dashboard.fno_270_2023_24 where toInt32(toFloat32(summa_na_bankovskih_chetah_v_inos_bankah))>0

union all

select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,

    concat('FNO240') as database,
    concat('Денежные средства') as aktivy,
    concat('Наличие') as oper,
    concat('Сумма денег на банковских счетах в иностранных банках') as dopinfo,
    concat('') as num_doc,
    toInt32(toFloat32(summa_deneg_na_bank_schetah_v_inos_bankah)) as summ
from pfr_dashboard.fno_240_2023_24 where toInt32(toFloat32(summa_deneg_na_bank_schetah_v_inos_bankah))>0


union all

select
    iin_bin,
    '' as iin_bin_pokup,
    '' as iin_bin_prod,
    toDate('2024-01-01') as date,
    concat('FNO250') as database,
    concat('Денежные средства') as aktivy,
    concat('Наличие') as oper,
    concat('Наличные ДС- Сумма;') as dopinfo,
    concat('') as num_doc,
    toInt32(toFloat32(summa_ne_previshaushaya_10k_krat_razmer_mrp_sum)) as summ

from pfr_dashboard.fno_250_2023_24 where toInt32(toFloat32(summa_ne_previshaushaya_10k_krat_razmer_mrp_sum))>0
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';
