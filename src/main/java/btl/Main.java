package btl;

import com.drew.imaging.ImageProcessingException;
import org.apache.commons.io.FileDeleteStrategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import java.io.*;

public class Main {
    private static int cnt = 1;
    private static ArrayList<Integer> order = new ArrayList<>();
    private static int[] orders = {
            123, 132, 213, 231, 312, 321
    };

    public static synchronized void main(String[] args) throws ImageProcessingException, IOException {
        String user = System.getProperty("user.dir");
        init(user);

        ArrayList<File[]> filesList = new ArrayList<File[]>();
        String rsc = user + "\\src\\main\\resources";
        String src = user + "\\src\\main\\resources\\images";
        String tar = user + "\\src\\main\\resources\\result";

        File srcF = new File(src);
        File tarF = new File(tar);

        loop:
        while (true) {
            Classification cls = null;
            Scanner in = new Scanner(System.in);
            System.out.println("-------------------------------------------------------");
            System.out.println("1. 정렬 순서 선택하기");
            System.out.println("2. 모든 경우의 수 확인하기");
            System.out.println("3. 종료하기");
            System.out.println("-------------------------------------------------------");
            System.out.print("선택해 주세요! ==> ");
            int i = in.nextInt();
            if (i >= 1 && i <= 3) {
                switch (i) {
                    case 1:
                        Path p = Paths.get(tar);
                        Files.createDirectory(p);
                        cls = new Classification(srcF, p.toFile());
                        boolean success = false;
                        System.out.println(" 1. 정렬 순서 선택하기(0.선택완료 1.시간 2.밝기 3.위치 4.초점거리)");
                        inputOrder();
                        cls.setOrder(order);
                        System.out.println();
                        System.out.println("처리중...........");
                        cls.start();
                        break;

                    // 모든 결과물 확인
                    case 2:
                        Dir[] dirs = initAll(user);
                        for(Dir dir : dirs) {
                            Path path = Paths.get(dir.getmFile().getPath());
                            cls = new Classification(srcF, path.toFile());
                            cls.setOrder(dir.getmOrder());
                            System.out.println();
                            System.out.println("처리중...........");
                            cls.start();
                            System.out.println("완료: " + intToString(dir.mOrder));
                        }
                        break;

                    case 3:
                        break loop;

                }
            } else {
                System.out.println(" 1~3 범위안에 숫자를 입력해주세요!!");
            }

        }
    }

    // 결과폴더 초기화 후 다시 생성
    public static void init(String userdir) {
        String src = userdir + "\\src\\main\\resources\\result";
        File main = new File(userdir + "\\src\\main\\resources");
        File srcFile = new File(src);

        if (main.exists()) {
            for (File f : main.listFiles()) {
                if (f.getName().contains("result")) {
                    srcFile = f;
                    try {
                        FileDeleteStrategy.FORCE.delete(f);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public static void checkTrue(boolean a) {
        if (!a) {
            System.out.println(" 동일한 숫자가 존재합니다! 순서를 맞춰주세요!");
        }
    }

    public static Dir[] initAll(String userdir) {
        init(userdir);
        String[] target = new String[6];
        String name = userdir + "\\src\\main\\resources\\result_";
        Dir[] dirs = new Dir[6];
        for (int i = 0; i < target.length; i++) {
            try {
                Path p = Paths.get(name + intToString(orders[i]));
                Files.createDirectory(p);
                dirs[i] = new Dir(p.toFile(), orders[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return dirs;
    }

    public static boolean add(ArrayList<Integer> order, int num) {
        if (!order.contains(num)) {
            order.add(num);
            return true;
        } else {
            return false;
        }
    }

    public static void inputOrder() {
        Scanner in = new Scanner(System.in);
        System.out.print(" " + cnt + "번째 숫자를 입력해 주세요. >>");
        int i = in.nextInt();
        if (i <= 4 && i > 0) {
            cnt++;
            order.add(i);
            inputOrder();
        } else if (cnt >= 4 || i == 0) {
            System.out.print("순서입력 완료 입력순서: ");
            for (int j : order) {
                System.out.print(j);
            }
            System.out.println();
        } else {
            System.out.println(" 오류! 다시 입력해주세요!");
            inputOrder();
        }
    }

    public static String intToString(int order) {
        String[] str = String.valueOf(order).split("");
        String full = "";
        for (String s : str) {
            s = change(s);
            full += s + "_";
        }
        return full;
    }

    private static String change(String str) {
        if (str.equals("1")) {
            return "시간";
        } else if (str.equals("2")) {
            return "밝기";
        } else if (str.equals("3")) {
            return "위치";
        }
        return null;
    }

    static class Dir {
        private File mFile;
        private int mOrder;

        Dir(File file, int order) {
            mFile = file;
            mOrder = order;
        }

        public void setmFile(File mFile) {
            this.mFile = mFile;
        }

        public void setmOrder(int mOrder) {
            this.mOrder = mOrder;
        }

        public File getmFile() {
            return mFile;
        }

        public int getmOrder() {
            return mOrder;
        }
    }
}
