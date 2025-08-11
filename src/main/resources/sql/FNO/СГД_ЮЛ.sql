SELECT *
FROM (
select
    bin as iin_bin,
    toDate('2024-01-01') as date,
    concat('FNO') as database,
    concat('СГД ЮЛ') as oper,
    concat('100_00_018:',  f_100_00_018,'; 100_00_055:',  f_100_00_055,'; 110_00_025:',  f_110_00_025,'; 110_00_056:',  f_110_00_056,'; 150_00_019:',  f_150_00_019,'; 150_00_054:',  f_150_00_054,
  '; 180_00_006:',f_180_00_006, '; 180_00_011:',f_180_00_011, '; 180_00_012:',f_180_00_012,'; 910_00_001:',  f_910_00_001,  '; 912_00_004:',  f_912_00_004, '; 913_00_001:',  f_913_00_001,  ';') as dopinfo,
    toInt32(toFloat32(godovoy_dohot_s_uchet_po_dannym_fno100f110f150f180f910)) as summ

from pfr_dashboard.dohot_ul where toInt32(toFloat32(godovoy_dohot_s_uchet_po_dannym_fno100f110f150f180f910))>0
) AS subquery
WHERE iin_bin = '$P-IIN'
AND date BETWEEN '$P-DATEFROM' AND '$P-DATETO';