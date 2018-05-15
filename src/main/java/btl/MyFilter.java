package btl;

import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicLine;
import net.sf.geographiclib.GeodesicMask;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

public class MyFilter {

    /**
     * Filter member
     */
    private GeoLocation mGeoLocation = null;

    private HashMap<String, File[]> mGpsMap;
    private HashMap<String, File[]> mDateMap;
    private HashMap<String, File[]> mBrightMap;
    private HashMap<String, File[]> mFocalLengthMap;
    private File mSrcFile;

    public MyFilter(File srcFile) {
            if (srcFile.isDirectory()) {
            mSrcFile = srcFile;
            mGpsMap = new HashMap<String, File[]>();
            mBrightMap = new HashMap<String, File[]>();
            mDateMap = new HashMap<String, File[]>();
            mFocalLengthMap = new HashMap<String, File[]>();
        } else {
            throw new IllegalArgumentException("폴더가 아님");
        }
    }

    public GeoLocation getLocation() {
        return mGeoLocation;
    }

    // 밝기 값을 double로 바꿔줌
    private static double convertBright(String str) {
        String[] result = str.split("/");
        if(result.length ==1) {
            return Double.parseDouble(result[0]);
        } else {
            return (Double.parseDouble(result[0])) / (Double.parseDouble(result[1]));
        }
    }
    // 거리측정
    private static double distanceBetween(GeoLocation start, GeoLocation end) {
        if (start != null && end != null) {
            Geodesic goed = Geodesic.WGS84;
            GeodesicLine line = goed.InverseLine(start.getLatitude(), start.getLongitude(), end.getLatitude(), end.getLongitude(),
                    GeodesicMask.DISTANCE_IN | GeodesicMask.LATITUDE | GeodesicMask.LONGITUDE);
            return line.Distance();
        }
        return -1;
    }
    // 날짜에서 Hour계산
    private static int getHours(String date) {
        String[] tSp = date.split(" ");
        String[] ttSp = tSp[1].split(":");
        return Integer.parseInt(ttSp[0]);
    }
    // 파일에서 밝기 추출
    private String extractBrightne(File f) {
        ExifSubIFDDirectory sub = null;
        try {
            sub = (ExifSubIFDDirectory) MyReader.getDir(MyReader.TAG_EXIF_SUBIFD, f);
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String bright;
        if(sub == null) {
            return null;
        } else {
            bright = sub.getString(ExifSubIFDDirectory.TAG_BRIGHTNESS_VALUE);
            if(bright != null) {
                return bright;
            }
        }
        return null;
    }
    // 파일에서 위치값 추출
    private static GeoLocation extractGeoLocation(File f) {
        GpsDirectory gps = null;
        try {
            gps = (GpsDirectory) MyReader.getDir(MyReader.TAG_EXIF_GPS, f);
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(gps == null) {
            return null;
        } else  {
            if(gps.getGeoLocation() == null) {
                return null;
            } else {
                return gps.getGeoLocation();
            }
        }
    }
    // 파일에서 날짜 추출
    private static int extractDateHour(File f) {
        ExifIFD0Directory ifd0 = null;
        try {
            ifd0 = (ExifIFD0Directory) MyReader.getDir(MyReader.TAG_EXIF_IFD0, f);
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(ifd0 != null) {
            String str = ifd0.getString(ExifIFD0Directory.TAG_DATETIME); // 이걸로 해야되 date는 다른나라 기준으로 나옴
            if(str != null) {
                return getHours(str);
            } else {
                return -1;
            }
        }
        return -1;
    }
    // 파일에서 초점 값 추출
   protected static String extractFocalLength(File f) {
        ExifSubIFDDirectory sub = null;
        try {
            sub = (ExifSubIFDDirectory) MyReader.getDir(MyReader.TAG_EXIF_SUBIFD, f);
        } catch (ImageProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String focalLength;
        if(sub == null) {
            return null;
        } else {
            focalLength = sub.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
            if(focalLength != null) {
                return focalLength;
            }
        }
        return null;
    }

    // GPS(1m) 필터
    public void gpsFilter() {
        ArrayList<File> fileList = new ArrayList<File>();

        //GPS값이 없는 파일을 묶어준다.
        if (mSrcFile.listFiles() != null) {
            File[] gpsNull = mSrcFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    GpsDirectory dir = null;
                    try {
                        dir = (GpsDirectory) MyReader.getDir(MyReader.TAG_EXIF_GPS, pathname);
                    } catch (ImageProcessingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(dir == null || dir.getGeoLocation() == null) {
                        return true;
                    }
                    return false;
                }
            });
            mGpsMap.put("null", gpsNull);
            fileList.addAll(Arrays.asList(mSrcFile.listFiles()));
            fileList.removeAll(Arrays.asList(gpsNull));
        } else {
            throw new NullPointerException();
        }

        //GPS 가 1미터내에 같으면, 묶어줌
        while(true) {
            if(fileList.size() == 0) {
                break;
            }
            File index = fileList.get(0);
            ArrayList<File> temp = new ArrayList<>();
            GpsDirectory gps = null;
            try {
                gps = (GpsDirectory) MyReader.getDir(MyReader.TAG_EXIF_GPS, index);
            } catch (ImageProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            for(File f : fileList) {
                if(distanceBetween(gps.getGeoLocation(), extractGeoLocation(f)) == 0 && distanceBetween(gps.getGeoLocation(), extractGeoLocation(f)) <=1) {
                    temp.add(f);
                }
            }
            fileList.removeAll(temp);
            File[] tFile = new File[temp.size()];
            tFile = temp.toArray(tFile);
            GeoLocation geoLocation = gps.getGeoLocation();
            double[] lat = GeoLocation.decimalToDegreesMinutesSeconds(geoLocation.getLatitude());
            double[] lng = GeoLocation.decimalToDegreesMinutesSeconds(geoLocation.getLongitude());
            String name = lat[0] + "\u00B0" + lat[1] + "'" + lat[2] + "'', " + lng[0] + "\u00B0" + lng[1] + "'" + lng[2] + "''";
            // 10진 --> 60진
            changeGPS(gps.getGeoLocation().toString());
           mGpsMap.put(name, tFile);
            if(fileList.size() == 0 ) break;
        }
    }
    // 밝기(양수,음수) 필터
    public void brightFilter() {
        ArrayList<File> fileList = new ArrayList<File>();

        if(mSrcFile.listFiles() != null) {
            fileList.addAll(Arrays.asList(mSrcFile.listFiles()));
            File[] nullBright = mSrcFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String bright = extractBrightne(pathname);
                    if(bright == null) {
                        return true;
                    }
                    return false;
                }
            });
            fileList.removeAll(Arrays.asList(nullBright));
            mBrightMap.put("null", nullBright);
        } else {
            return;
        }

        ArrayList<File> temp = new ArrayList<>();
        for(File f : fileList) {
            String brightness = extractBrightne(f);
            // temp는 0보다큰 밝기 값
            if(convertBright(brightness) > 0) {
                temp.add(f); }
        }
        fileList.removeAll(temp);

        File[] tFile = new File[temp.size()];
        tFile = temp.toArray(tFile);
        mBrightMap.put("positive", tFile);
        tFile = new File[fileList.size()];
        tFile = fileList.toArray(tFile);
        mBrightMap.put("negative", tFile);

    }
    // 날짜(1시간) 필터
    public void dateFilter() {
        ArrayList<File> fileList = new ArrayList<File>();

        if(mSrcFile.listFiles() != null) {
            fileList.addAll(Arrays.asList(mSrcFile.listFiles()));
            File[] nullDate = mSrcFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    int date = extractDateHour(pathname);
                    if(date == -1) {
                        return true;
                    }
                    return false;
                }
            });
            fileList.removeAll(Arrays.asList(nullDate));
            mFocalLengthMap.put("null", nullDate);
        } else {
            return;
        }

        while(true) {
            if(fileList.size() == 0) {
                break;
            }
            ArrayList<File> temp = new ArrayList<>();
            int main = extractDateHour(fileList.get(0));
            for (File f : fileList) {
                if(main == extractDateHour(f)) {
                    temp.add(f);
                }
            }
            fileList.removeAll(temp);
            File[] tFile = new File[temp.size()];
            tFile = temp.toArray(tFile);
            mDateMap.put(Integer.toString(main)+"시", tFile);
            if(fileList.size() == 0) break;
        }

    }
    // 초점 필터
    public void focalLengthFilter() {
        ArrayList<File> fileList = new ArrayList<File>();

        if(mSrcFile.listFiles() != null) {
            fileList.addAll(Arrays.asList(mSrcFile.listFiles()));
            File[] nullDate = mSrcFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String str = extractFocalLength(pathname);
                    if(str == null) {
                        return true;
                    }
                    return false;
                }
            });
            fileList.removeAll(Arrays.asList(nullDate));
            mFocalLengthMap.put("null", nullDate);
        } else {
            return;
        }

        Optional<File> a = fileList.stream().min((o1, o2) -> {
            double f01 = Double.parseDouble(extractFocalLength(o1));
            double f02 = Double.parseDouble(extractFocalLength(o2));
            if(f01-f02 >= 0) {
                return 1;
            } else {
                return -1;
            }
        });

        double start = findMin(a.get());
        double end = start+0.5;
        while(true) {
            if(fileList.size() == 0) {
                break;
            }
            ArrayList<File> temp = new ArrayList<>();
            for(File f : fileList) {
                String fl = extractFocalLength(f);
                double dfl = Double.parseDouble(fl);
                if(dfl>= start && dfl<end) {
                    temp.add(f);
                }
            }
            String name = "FocalLength " + start + "-" + end;
            start+= 0.5;
            end+= 0.5;
            if(temp.size() == 0) continue;
            fileList.removeAll(temp);
            File[] tFile = new File[temp.size()];
            tFile = temp.toArray(tFile);

            mFocalLengthMap.put(name, tFile);
            if(fileList.size() == 0) break;
        }
    }

    private String changeGPS(String str) {
        String[] latlng = str.split(",");
        if(latlng.length ==2) {
            double lat = Double.parseDouble(latlng[0]);
            double lng = Double.parseDouble(latlng[1]);
            return GeoLocation.decimalToDegreesMinutesSecondsString(lat)+","+GeoLocation.decimalToDegreesMinutesSecondsString(lng);
        } else {
            return str;
        }
    }
    private double findMin(File f) {
        double min = Double.parseDouble(extractFocalLength(f));

        double a = min - (int) min;
        if(a>=0.5) {
            return min - a + 0.5;
        } else {
            return min - a;
        }
    }
    public Map<String, File[]> getGpsMap() { return mGpsMap; }
    public Map<String, File[]> getBrightMap() { return mBrightMap; }
    public Map<String, File[]> getDateMap() { return mDateMap; }
    public Map<String, File[]> getFocalLengthMap() { return mFocalLengthMap; }

}
