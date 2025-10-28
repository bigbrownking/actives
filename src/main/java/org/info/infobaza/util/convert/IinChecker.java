package org.info.infobaza.util.convert;

import org.springframework.stereotype.Component;

@Component
public class IinChecker {
    private static char[] IIN_ALLOWED = new char[]{'0', '1', '2', '3'};
    public static boolean isUl(String iin) {
        char fifthChar = iin.charAt(4);
        for (char allowed : IIN_ALLOWED) {
            if (fifthChar == allowed) {
                return false;
            }
        }
        return true;
    }
}
