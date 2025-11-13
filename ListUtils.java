import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ListUtils {

    public static <T> boolean hasIntersectionOptimized(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<>(list2); 
        return list1.stream().anyMatch(set::contains);
    }
}