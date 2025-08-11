select *
from (
select

    owner_iin_bin as iin_bin,
    registration_date as date,
    concat('Сведения МСХ') as database,
    concat('Спецтехника') as aktivy,
    concat('Приобретение') as oper,
concat('Вид:', lowerUTF8(equipment_spec),'; Тип:', lowerUTF8(equipment_type), '; Форма:', lowerUTF8(equipment_form), '; Год:', issue_year, ';') as dopinfo,
toInt32('0') as summ
from pfr_dashboard.texnika   where end_date is null

union all

select

    owner_iin_bin as iin_bin,
    end_date as date,
    concat('Сведения МСХ') as database,
    concat('Спецтехника') as aktivy,
    concat('Реализация') as oper,
concat('Вид:', lowerUTF8(equipment_spec),'; Тип:', lowerUTF8(equipment_type), '; Форма:', lowerUTF8(equipment_form), '; Год:', issue_year, ';') as dopinfo,
toInt32('0') as summ
from pfr_dashboard.texnika   where end_date is not null

) as subquery
where iin_bin = '$P-IIN'
and date between '$P-DATEFROM' and '$P-DATETO';