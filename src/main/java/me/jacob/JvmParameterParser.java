package me.jacob;

import me.jacob.entities.JvmParameterType;

import java.util.ArrayList;
import java.util.List;

public class JvmParameterParser {

    public List<JvmParameterType> parseParameters(String paramSection) {
        List<String> jvmParams = new ArrayList<>();
        StringBuilder currentType = new StringBuilder();

        // Traverse the parameter string and parse each type
        for (int i = 0; i < paramSection.length(); i++) {
            char c = paramSection.charAt(i);

            // Handle arrays (including multidimensional arrays)
            if (c == '[') {
                currentType.append(c);
                // Keep appending '[' to handle multidimensional arrays
                while (paramSection.charAt(i + 1) == '[') {
                    i++;
                    currentType.append('[');
                }
                // After array brackets, parse the element type (primitive or object)
                i++;
                c = paramSection.charAt(i);
            }

            if (c == 'L') {
                // Object type (e.g., Ljava/lang/String;)
                currentType.append(c);
                // Collect characters until we hit the ending semicolon
                while (paramSection.charAt(++i) != ';') {
                    currentType.append(paramSection.charAt(i));
                }
                currentType.append(';');
                jvmParams.add(currentType.toString());
                currentType.setLength(0); // Reset currentType for the next parameter
            } else {
                // Primitive type (e.g., I, Z, D, etc.)
                currentType.append(c);
                jvmParams.add(currentType.toString());
                currentType.setLength(0); // Reset currentType for the next parameter
            }
        }

        List<JvmParameterType> result = new ArrayList<>();
        for (var param : jvmParams) {
            result.add(parseType(param));
        }

        return result;
    }

    public JvmParameterType parseType(String param) {
        switch (param) {
            case "I":
                return new JvmParameterType("int");
            case "Z":
                return new JvmParameterType("boolean");
            case "B":
                return new JvmParameterType("byte");
            case "C":
                return new JvmParameterType("char");
            case "D":
                return new JvmParameterType("double");
            case "F":
                return new JvmParameterType("float");
            case "J":
                return new JvmParameterType("long");
            case "S":
                return new JvmParameterType("short");
            case "V":
                return new JvmParameterType("void");
            default:
                if (param.startsWith("L")) {
                    // Object type: Extract fully qualified name (e.g., Ljava/lang/Object; or Lcom/example/Abc$Builder;)
                    String fullyQualifiedName = param.substring(1, param.length() - 1); // Remove 'L' and ';'

                    // Replace JVM nested class indicator `$` with `.` to reflect Java's nested class notation
                    String formattedName = fullyQualifiedName.replace('$', '.');

                    // Extract the simple name (e.g., Abc.Builder from com.example.Abc.Builder)
                    String simpleName = formattedName.substring(formattedName.lastIndexOf('/') + 1);

                    return new JvmParameterType(simpleName);
                } else if (param.startsWith("[")) {
                    // Array type: Calculate dimensions and parse the base type
                    int dimensions = 0;
                    while (param.charAt(dimensions) == '[') {
                        dimensions++;
                    }
                    // Parse the base type (after the array brackets)
                    String baseTypeDescriptor = parseType(param.substring(dimensions)).getTypeName();
                    return new JvmParameterType(baseTypeDescriptor, dimensions);
                } else {
                    throw new IllegalArgumentException("Unknown JVM parameter type: " + param);
                }
        }
    }
}
