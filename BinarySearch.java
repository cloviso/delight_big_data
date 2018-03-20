import java.util.ArrayList;
import java.util.List;

public class BinarySearch {
    public static void main(String[] args)
    {
        System.out.println("御宅胖猫你躲哪里去了............");
        List<Integer> arrayList = new ArrayList<Integer>();
        arrayList.add(11);
        arrayList.add(122);
        arrayList.add(133);
        arrayList.add(1444);
        arrayList.add(1991);

        binarySearch(arrayList,199111);

        System.out.println(binarySearch(arrayList,19911111)?("找到胖猫了"):("抱歉，查无此猫"));

    }

    /**
     * 二分搜索一个递归Java实现
     * @param numbers
     * @param value
     * @return
     */
    public static boolean binarySearch(final List<Integer> numbers, final Integer value){
        if(numbers == null || numbers.isEmpty()){
            return false;
        }

        final Integer comparison = numbers.get(numbers.size()/2);
        if(value.equals(comparison)){
            return true;
        }

        if(value < comparison){
            return binarySearch(numbers.subList(0,numbers.size()/2),value);
        } else{
            return binarySearch(numbers.subList(numbers.size()/2 +1,numbers.size()),value);
        }

    }


}