package org.info.infobaza.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.info.infobaza.model.dossierprime.*;
import org.info.infobaza.repository.dossierprime.DossierFlRepository;
import org.info.infobaza.repository.dossierprime.DossierPhotoRepository;
import org.info.infobaza.repository.dossierprime.DossierUlRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DossierService {
    private final DossierUlRepository dossierUlRepository;
    private final DossierFlRepository dossierFlRepository;
    private final DossierPhotoRepository dossierPhotoRepository;

    public MvFlWithPhotoDto getMvFl(String iin) {
        MvFlWithPhotoDto mvFlWithPhotoDto = new MvFlWithPhotoDto();
        List<MvFl> myMv_fl = dossierFlRepository.getUsersByLike(iin);
        mvFlWithPhotoDto = tryAddPhotoToDto(mvFlWithPhotoDto, iin);
        if (!myMv_fl.isEmpty()) {
            mvFlWithPhotoDto.setMvFlList(myMv_fl);
        }
        return mvFlWithPhotoDto;
    }

    private MvFlWithPhotoDto tryAddPhotoToDto(MvFlWithPhotoDto fl, String IIN) {
        try {
            List<PhotoDb> photos = dossierPhotoRepository.findAllByIin(IIN);
            List<PhotoDb> photoDbs = new ArrayList<>();
            for (PhotoDb photoDb1 : photos) {
                photoDbs.add(photoDb1);
                fl.setPhotoDbs(photoDbs);
            }
            return fl;
        } catch (Exception e) {
            log.error("Error: ", e);
        }
        return fl;
    }

    public List<ULDto> findUlByBin(String bin) {
        Optional<MvUl> ul;
        try {
            ul = dossierUlRepository.getUlByBin(bin);
        } catch (Exception e) {
            log.error("Error occurred while fetching findUlByBin by bin: {}", bin, e);
            return null;
        }
        List<ULDto> list = new ArrayList<>();
        ULDto ulDto = new ULDto();
        if (ul.isPresent()) {
            MvUl ulEntity = ul.get();
            ulDto.setBin(bin);
            ulDto.setFullName(ulEntity.getFull_name_rus());
            ulDto.setStatus(ulEntity.getUl_status());
            ulDto.setRegDate(ulEntity.getOrg_reg_date());
            ulDto.setIsResident(ulEntity.getIs_resident() != null ? ulEntity.getIs_resident().equals("1") ? true : false : false);
        } else {
            return null;
        }
        list.add(ulDto);

        return list;
    }
}
