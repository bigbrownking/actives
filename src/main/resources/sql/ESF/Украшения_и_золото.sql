SELECT *
FROM (
select
	IIN_SELLER as iin_bin,
	IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Украшения') as aktivy,
    concat('Реализация') as oper,
    concat('Украшения; Описание:',lowerUTF8(DESCRIPTION),'; Покупатель:', IIN_CUSTOMER,';') as dopinfo,
    concat('') as num_doc,
    abs(toInt64(SUMMA_PRIOBRETENIE_V_TENGE)) as summ
from pfr_dashboard.ukrashenia_esf_2023_24
where not match(lowerUTF8(DESCRIPTION),'золот.*слит')

union all

select
	IIN_SELLER as iin_bin,
	IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Золото') as aktivy,
    concat('Реализация') as oper,
    concat('Золото; Описание:',lowerUTF8(DESCRIPTION),'; Покупатель:', IIN_CUSTOMER,';') as dopinfo,
    concat('') as num_doc,
    abs(toInt64(SUMMA_PRIOBRETENIE_V_TENGE)) as summ
from pfr_dashboard.ukrashenia_esf_2023_24
where match(lowerUTF8(DESCRIPTION),'золот.*слит')

union all

select
	IIN_CUSTOMER as iin_bin,
    IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Украшения') as aktivy,
    concat('Приобретение') as oper,
    concat('Украшения; ', 'Описание:',lowerUTF8(DESCRIPTION), '; Продовец: ', IIN_SELLER,';') as dopinfo,
    concat('') as num_doc,
    abs(toInt64(SUMMA_PRIOBRETENIE_V_TENGE)) as summ
from pfr_dashboard.ukrashenia_esf_2023_24
where not match(lowerUTF8(DESCRIPTION),'золот.*слит')

union all

select
	IIN_CUSTOMER as iin_bin,
    IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Золото') as aktivy,
    concat('Приобретение') as oper,
    concat('Золото; ', 'Описание:',lowerUTF8(DESCRIPTION), '; Продовец: ', IIN_SELLER,';') as dopinfo,
    concat('') as num_doc,
    abs(toInt64(SUMMA_PRIOBRETENIE_V_TENGE)) as summ
from pfr_dashboard.ukrashenia_esf_2023_24
where match(lowerUTF8(DESCRIPTION),'золот.*слит')
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';
