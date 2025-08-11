SELECT *
FROM (
select

    SELLER_MAINCODE as iin_bin,
    toDate(DATE_OPER) as date,
    concat('FM-1') as database,
    concat('Недвижимое имущество') as aktivy,
    concat('Реализация') as oper,
    concat('Сумма:',cast(OPER_TENGE_AMOUNT as int),';ID сообщения:',MESS_ID,';Код вида операции:',OPER_IDVIEW, '; Номер операции:',OPER_NUMBER,';') as dopinfo,
    cast(OPER_TENGE_AMOUNT as int) as summ

from (

select distinct
    MESS_ID,
    OPER_IDVIEW,
    OPER_TENGE_AMOUNT,
    OPER_NUMBER,
    DATE_OPER,

    SELLER_MAINCODE

from pfr_dashboard.asloy_joined_table where DATE_OPER>='2023-01-01' and DATE_OPER<='2023-12-31' and (OPER_IDVIEW='1811' or OPER_SUSP='1092' or OPER_SUSP='9014' or OPER_IDTYPE='721' or OPER_IDTYPE='722')
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3') and SELLER_MAINCODE is not null and SELLER_MAINCODE!='')


union all

select

    CUSTOMER_MAINCODE as iin_bin,
    toDate(DATE_OPER) as date,
    concat('FM-1') as database,
    concat('Недвижимое имущество') as aktivy,
    concat('Приобретение') as oper,
    concat('Сумма:',cast(OPER_TENGE_AMOUNT as int),';ID сообщения:',MESS_ID,';Код вида операции:',OPER_IDVIEW, '; Номер операции:',OPER_NUMBER,';') as dopinfo,
    cast(OPER_TENGE_AMOUNT as int) as summ

from (

select distinct
    MESS_ID,
    OPER_IDVIEW,
    OPER_TENGE_AMOUNT,
    OPER_NUMBER,
    DATE_OPER,

    CUSTOMER_MAINCODE

from pfr_dashboard.asloy_joined_table where DATE_OPER>='2023-01-01' and DATE_OPER<='2023-12-31' and (OPER_IDVIEW='1811' or OPER_SUSP='1092' or OPER_SUSP='9014' or OPER_IDTYPE='721' or OPER_IDTYPE='722')
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3') and CUSTOMER_MAINCODE is not null and CUSTOMER_MAINCODE!='')
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';