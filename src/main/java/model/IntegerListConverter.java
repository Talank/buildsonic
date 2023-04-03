package model;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Converter
public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {
    private static final String SPLIT_CHAR = "\n";

    @Override
    public String convertToDatabaseColumn(List<Integer> intList) {
        if (intList == null || intList.size() == 0) {
            return null;
        } else {
            List<String> stringList = new ArrayList<>(intList.size());
            for (Integer myInt : intList) {
                stringList.add(String.valueOf(myInt));
            }
            return (intList == null)? null : String.join(SPLIT_CHAR, stringList);
        }
    }

    @Override
    public List<Integer> convertToEntityAttribute(String string) {
        if (string == null || string == "") {
            return null;
        } else {
            int[] ints = Stream.of(string.split(SPLIT_CHAR)).mapToInt(Integer::parseInt).toArray();
            List<Integer> intList = new ArrayList<Integer>(ints.length);
            for (int i : ints)
            {
                intList.add(i);
            }
            return intList;
        }
    }
}
