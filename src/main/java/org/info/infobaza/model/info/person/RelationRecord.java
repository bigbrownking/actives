package org.info.infobaza.model.info.person;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Data
@AllArgsConstructor
public class RelationRecord {
    private String iin_1;
    private String iin_2;
    private String vid_sviazi;
    private String status;
    private int level_rod;
    private Map<String, String> dopinfo;

    public RelationRecord(String iin_1, String iin_2, String vid_sviazi, String status, Map<String, String> dopinfo) {
        this.iin_1 = iin_1;
        this.iin_2 = iin_2;
        this.vid_sviazi = vid_sviazi;
        this.status = status;
        this.dopinfo = dopinfo;
    }

    public RelationRecord(String iin1, String iin2, String vidSviazi, String status, int levelRod) {
        this.iin_1 = iin1;
        this.iin_2 = iin2;
        this.vid_sviazi = vidSviazi;
        this.status = status;
        this.level_rod = levelRod;
        this.dopinfo = new HashMap<>();
    }
}