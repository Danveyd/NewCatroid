package org.catrobat.catroid.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class ChEnginePayloadHelper {
    private static final byte[] MAGIC_MARKER = "CHENGINE_MAGIC_PAYLOAD_V1".getBytes();


    public static void hideZipInAudio(File realAudio, File zipPayload, File output) throws IOException {
        try (FileInputStream fisAudio = new FileInputStream(realAudio);
             FileInputStream fisZip = new FileInputStream(zipPayload);
             FileOutputStream fos = new FileOutputStream(output)) {

            byte[] buffer = new byte[8192];
            int length;


            while ((length = fisAudio.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }


            fos.write(MAGIC_MARKER);


            while ((length = fisZip.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }


    public static boolean extractPayloadFromAudio(File fakeAudioFile, File outputZip) throws IOException {
        try (FileInputStream fis = new FileInputStream(fakeAudioFile)) {

            int b;
            int matchIndex = 0;


            while ((b = fis.read()) != -1) {
                if (b == MAGIC_MARKER[matchIndex]) {
                    matchIndex++;
                    if (matchIndex == MAGIC_MARKER.length) {


                        try (FileOutputStream fos = new FileOutputStream(outputZip)) {
                            byte[] buffer = new byte[8192];
                            int len;
                            while ((len = fis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }
                        return true;
                    }
                } else {

                    matchIndex = (b == MAGIC_MARKER[0]) ? 1 : 0;
                }
            }
        }

        return false;
    }
}