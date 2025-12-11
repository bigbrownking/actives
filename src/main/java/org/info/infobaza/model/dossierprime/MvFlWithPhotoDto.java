package org.info.infobaza.model.dossierprime;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MvFlWithPhotoDto {
    private List<MvFl> mvFlList;
    private List<PhotoDb> photoDbs;
}
