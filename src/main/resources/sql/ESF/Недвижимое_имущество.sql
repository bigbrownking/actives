select *
from(
select
	IIN_SELLER as iin_bin,
	IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Недвижимое имущество') as aktivy,
    concat('Реализация') as oper,
    concat('Описание:',lowerUTF8(DESCRIPTION), '; Покупатель:', IIN_CUSTOMER,';') as dopinfo,
    concat('') as num_doc,
    abs(toInt64(SUMMA_PRIOBRETENIE_V_TENGE)) as summ
from pfr_dashboard.nedvijka_esf_2023_24

union all

select
	IIN_CUSTOMER as iin_bin,
    IIN_CUSTOMER as iin_bin_pokup,
    IIN_SELLER as iin_bin_prod,
    toDate(INVOICE_DATE) as date,
    concat('ESF') as database,
    concat('Недвижимое имущество') as aktivy,
    concat('Приобретение') as oper,
    concat('Описание:',lowerUTF8(DESCRIPTION), '; Продовец: ', IIN_SELLER,';') as dopinfo,
    concat('') as num_doc,
    abs(toInt64(SUMMA_PRIOBRETENIE_V_TENGE)) as summ
from pfr_dashboard.nedvijka_esf_2023_24

) as subquery
where iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';
