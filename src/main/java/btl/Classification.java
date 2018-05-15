package btl;

import org.apache.commons.io.FileDeleteStrategy;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Classification {

    private static final int DATE_FILTER = 1;
    private static final int BRIGHT_FILTER = 2;
    private static final int GPS_FILTER = 3;
    private static final int FOCALLENGTH_FILTER =4;

    private File mSrcFile;
    private File mTarFile;
    private ArrayList<Integer> mOrder;
    private Map<String, File[]> mMap;
    private boolean mSuccess = false;
    private Thread running;

    public Classification(File srcFile, File tartFile) {
        if(srcFile.isDirectory() && tartFile.isDirectory()) {
            mSrcFile = srcFile;
            mTarFile = tartFile;
            mOrder = new ArrayList<>();
        } else {
            throw new IllegalArgumentException();
        }
    }

    public void makeDirs(Map<String, File[]> map) {
        Iterator<String> itr = map.keySet().iterator();

        while(itr.hasNext()) {
            String tar = itr.next();
            File[] files = map.get(tar);
            String src = mTarFile.getPath() +"\\"+ tar;
            Path path = Paths.get(src);
            File f = new File(src);
            try {
                Files.createDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            int cnt = 0;
            for (File file : files) {
                Path target = Paths.get(path.toString(), file.getName());
                try {
                    if (mSrcFile.getName().equals("images")) {
                        cnt++;
                        Files.copy(file.toPath(), target);
                    } else {
                        cnt++;
                        Files.move(file.toPath(), target);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(cnt == 0) {
                try {
                    FileDeleteStrategy.FORCE.delete(new File(path.toString()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setOrder(int...order) {
        for(int i : order) {
            mOrder.add(i);
        }
    }
    public void setOrder(int order) {
        if(order>100) {
            String[] str = String.valueOf(order).split("");
            for(String s : str) {
                mOrder.add(Integer.valueOf(s));
            }
        } else {
            throw new IllegalArgumentException();
        }
    }
    public void setOrder(ArrayList<Integer> order) {
        mOrder = order;
    }

    //TODO 마지막 3개의 순서를 두고 필터링 태그받으삼
    private void filtering(int tag) throws IllegalAccessException {

        File[] src = mSrcFile.listFiles();
        File[] tar = mTarFile.listFiles();

        MyFilter f = null;

        // 소스폴더(이미지 존재하는) 설정, 필터 생성
        if(hasImages(src)) {
            f = new MyFilter(mSrcFile);
        }

        // 사진에 디렉토리가 있는지 없는지 확인
        if(tar == null) {
            throw new NullPointerException();
        } else {
            if(tar.length == 0 || hasImages(tar) ) {
                // TODO 필터링
                filter(f,tag);
                this.makeDirs(mMap);
                // src, tar 바꿔줘야대
                //
                //
            } else {

                if(!hasImages(tar)) {
                    for(File dir : tar) {
                        // TODO dir에 이미지가 없으면 어떻게 할꺼야??
                        if(dir.listFiles().length == 0) {
                            continue;
                        }
                        mSrcFile = dir;
                        mTarFile = dir;
                        filtering(tag);
                    }
                    mTarFile = tar[0].getParentFile();

                }

            }
        }
    }

    private void filter(MyFilter filter, int tag) {
        if(filter != null) {
            switch (tag) {
                case DATE_FILTER:
                    filter.dateFilter();
                    mMap = filter.getDateMap();
                    break;
                case BRIGHT_FILTER:
                    filter.brightFilter();
                    mMap = filter.getBrightMap();
                    break;
                case GPS_FILTER:
                    filter.gpsFilter();
                    mMap = filter.getGpsMap();
                    break;
                case FOCALLENGTH_FILTER:
                    filter.focalLengthFilter();
                    mMap = filter.getFocalLengthMap();
                    break;
            }
        }
    }

    // File[] 이 전부 이미지만 있는지? src 폴더로 바꿔도 되는지 체크
    private boolean hasImages(File[] files) {
        for(File f : files) {
            String name = f.getName();
            if (f.isDirectory()) {
                return false;
            }
            if(!isJpeg(f)) return false;
        }
        return true;
    }

    // File이 jpeg인지 확인(exif 존재할 가능성이 있는지)
    private boolean isJpeg(File f) {
        String a = f.getName();
        int idx = a.lastIndexOf(".");
        String ext = a.substring(idx+1);
        return ext.equals("jpg") || ext.equals("jpeg");
    }

    public synchronized void start() {
        if(mOrder == null) {
            System.out.println("순서를 입력 오류!");
        } else {
            mSuccess = false;
            Iterator itr = mOrder.iterator();

            while (itr.hasNext()) {
                int num = (int) itr.next();
                try {
                    filtering(num);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            mSuccess = true;
            System.out.println("-------------------------------------------------------");
        }
    }

}
