import java.util.*;

public class Test {
    public static int getNum(int[] ary){
        System.out.println(ary.length);
        int count = 0;
        Map<Integer, List<Integer>> map = new HashMap<>();
        for(int i = 0; i < ary.length; i++) {
            if(map.get(ary[i]) == null) {
                map.put(ary[i], new ArrayList<Integer>());
            }
            map.get(ary[i]).add(i);
        }

        for (Map.Entry<Integer, List<Integer>> item : map.entrySet()) {
            List<Integer> list = item.getValue();

            if(list.size() <= 1) {
                continue;
            }
            int i = 1;
            System.out.println(list.size());
            while (i < list.size()) {
                if(list.get(i) - list.get(i - 1) > 1) {
                    count++;
                }
                i += 2;
            }
        }
        System.out.println(count);
        if(count ==0) {
            count = 1;
        }
        return count;
    }

    public static void main(String[] args) {
        /*
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        for(int i = 0; i < n; i++) {
            int m = sc.nextInt();
            int[] ary = new int[m];
            for(int j = 0; j < m; j++){
                ary[j] = sc.nextInt();
            }
        }

         */
        int[] ary = new int[]{1,2,1,2};
        int num = getNum(ary);
        System.out.println(num);
    }
}