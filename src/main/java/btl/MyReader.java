package btl;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MyReader {
    private File mFile;
    private String mPath;
    private Metadata mMetadata;
    public static final int TAG_EXIF_IFD0 = 0;
    public static final int TAG_EXIF_SUBIFD = 1;
    public static final int TAG_EXIF_GPS = 2;

    public MyReader(String pathname) throws ImageProcessingException, IOException {
        mPath = pathname;
        mFile = new File(mPath);
        mMetadata = ImageMetadataReader.readMetadata(mFile);
    }

    public MyReader(File file) throws ImageProcessingException, IOException {
        mFile = file;
        mPath = file.getAbsolutePath();
        mMetadata = ImageMetadataReader.readMetadata(mFile);
    }

    public boolean canRead() {
        if (mMetadata != null && mMetadata.getDirectoryCount() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public Directory getDir(Class<Directory> T) {
        return mMetadata.getFirstDirectoryOfType(T);
    }

    public static Directory getDir(int tag, File f) throws ImageProcessingException, IOException {
        Metadata metadata = null;
        switch (tag) {
            case TAG_EXIF_IFD0:
                metadata = ImageMetadataReader.readMetadata(f);
                return metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            case TAG_EXIF_SUBIFD:
                metadata = ImageMetadataReader.readMetadata(f);
                return metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            case TAG_EXIF_GPS:
                metadata = ImageMetadataReader.readMetadata(f);
                return metadata.getFirstDirectoryOfType(GpsDirectory.class);
            default:
                System.out.println("태그번호 확인해봐!");
                return null;
        }
    }
}
