select
    iin_owners as iin_bin,
    date_of_registration as date,
    concat('КАП МВД-Cведения по владельцам Авто') as database,
    concat('Транспортные средства') as aktivy,
    concat('Наличие') as oper,
    concat('Авто:', marka, '; Год авто:', year_of_release, '; ГРНЗ', registration_number,';') as dopinfo,
    toInt32(concat('0')) as summ
from pfr_dashboard.auto_VIN_vladelcy
where iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';