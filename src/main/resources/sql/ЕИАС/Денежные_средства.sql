SELECT *
FROM (
select

    MEMBER_MAINCODE as iin_bin,
    toDate(DATE_OPER) as date,
    concat('ЕИАС') as database,
    concat('Денежные средства') as aktivy,
    concat('Наличие') as oper,
    concat('Снятие ДС; ID сообщения:',MESS_ID,';Код вида операции:',OPER_IDVIEW, '; Номер операции:',OPER_NUMBER,';') as dopinfo,
    cast(OPER_TENGE_AMOUNT as int) as summ

from (

select distinct
    MESS_ID,
    OPER_IDVIEW,
    OPER_TENGE_AMOUNT,
    OPER_NUMBER,
    DATE_OPER,

    MEMBER_MAINCODE

from asloy_joined_table where DATE_OPER>='2023-01-01' and DATE_OPER<='2023-12-31' and (OPER_IDVIEW='311' or OPER_IDVIEW='321' or OPER_IDVIEW='511' or OPER_IDVIEW='530')
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3') and MEMBER_MAINCODE is not null and MEMBER_MAINCODE!='' )
where iin_bin = '$P-IIN';
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';
