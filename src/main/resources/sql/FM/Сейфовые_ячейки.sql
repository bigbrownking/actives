SELECT *
FROM (
select

    MEMBER_MAINCODE as iin_bin,
    toDate(DATE_OPER) as date,
    concat('FM') as database,
    concat('Сейфовые ячейки') as aktivy,
    concat('Наличие') as oper,
    concat('Сумма:',cast(OPER_TENGE_AMOUNT as int),';ID сообщения:',MESS_ID,';Код вида операции:',OPER_IDVIEW, '; Номер операции:',OPER_NUMBER,';') as dopinfo,
    cast(OPER_TENGE_AMOUNT as int) as summ
from (


select distinct
    MESS_ID,
    OPER_IDVIEW,
    OPER_TENGE_AMOUNT,
    OPER_NUMBER,
    DATE_OPER,

    MEMBER_MAINCODE

from pfr_dashboard.asloy_joined_table where DATE_OPER>='2023-01-01' and DATE_OPER<='2023-12-31' and lowerUTF8(dopinfo) not like '%сейфул%' and
(( lowerUTF8(dopinfo) like '%сейф%' and lowerUTF8(dopinfo) like '%ячейк%' ) or  (lowerUTF8(dopinfo) like '%сейф%' and lowerUTF8(dopinfo) like '%ячеек%')  or
 (lowerUTF8(dopinfo) like '%банк%' and lowerUTF8(dopinfo) like '%ячейк%')  or ( lowerUTF8(dopinfo) like '%банк%' and lowerUTF8(dopinfo) like '%ячеек%' )  )
and not(MESS_REASON_CODE='12' or MESS_REASON_CODE='13' or MESS_REASON_CODE='14' or MESS_OPER_STATUS_CODE='2' or MESS_OPER_STATUS_CODE='3') and MEMBER_MAINCODE is not null and MEMBER_MAINCODE!='')
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';